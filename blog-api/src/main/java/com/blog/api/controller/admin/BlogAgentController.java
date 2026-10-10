package com.blog.api.controller.admin;

import com.blog.api.common.Result;
import com.blog.api.platform.agent.AgentExecutor;
import com.blog.api.platform.agent.AgentMessage;
import com.blog.api.platform.agent.AgentRequest;
import com.blog.api.platform.model.AiMessage;
import com.blog.api.platform.service.MessageStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 智能体对话接口。
 * <p>用 SseEmitter（Spring MVC 原生 SSE）实现流式推送，避免 Flux/StreamingResponseBody 的 async 缓冲问题。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/agent")
@RequiredArgsConstructor
public class BlogAgentController {

    /** SSE 连接超时（10 分钟，覆盖智能体最长执行时间） */
    private static final long SSE_TIMEOUT_MS = 10 * 60 * 1000L;

    private final AgentExecutor agentExecutor;
    private final MessageStoreService messageStoreService;

    /** 共享调度器（避免每次新建） */
    private static final ScheduledExecutorService HEARTBEAT =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "sse-heartbeat");
                t.setDaemon(true);
                return t;
            });

    @PostMapping("/chat")
    public ResponseEntity<Result<Map<String, Object>>> chat(@RequestBody AgentRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(Result.error(400, "消息不能为空"));
        }
        log.info("智能体对话: sessionId={}", request.getSessionId());

        AgentExecutor.AgentResult result = agentExecutor.execute(
                request.getMessage(), request.getSessionId(), request.getUserId());

        return ResponseEntity.ok(Result.success(Map.of(
                "sessionId", result.sessionId(),
                "runId", result.runId(),
                "reply", "sessionId=" + result.sessionId() + ",runId=" + result.runId(),
                "latencyMs", result.latencyMs())));
    }

    /**
     * 智能体对话（SSE 流式）—— 用 SseEmitter。
     * <p>每个 AgentMessage 立即 send，10 分钟超时；定时心跳保持连接活跃。
     * 完成后 complete，异常时 completeWithError。</p>
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody AgentRequest request) {
        log.info("智能体流式对话(SSE): sessionId={}", request.getSessionId());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        // 完成/超时回调
        emitter.onCompletion(() -> log.debug("SSE 完成 sessionId={}", request.getSessionId()));
        emitter.onTimeout(() -> {
            log.warn("SSE 超时 sessionId={}", request.getSessionId());
            emitter.complete();
        });
        emitter.onError(e -> log.warn("SSE 错误 sessionId={}: {}", request.getSessionId(), e.getMessage()));

        // 心跳：每 15 秒发一个心跳保持连接活跃
        final long[] lastSendAt = {System.currentTimeMillis()};
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sse-heartbeat");
            t.setDaemon(true);
            return t;
        });
        Runnable heartbeatTask = () -> {
            try {
                if (System.currentTimeMillis() - lastSendAt[0] < 20000) return;
                emitter.send(SseEmitter.event().name("HEARTBEAT").data("ping"));
            } catch (Exception ignore) {
                // 客户端断开时 send 会抛异常，忽略即可
            }
        };
        scheduler.scheduleAtFixedRate(heartbeatTask, 15, 15, TimeUnit.SECONDS);

        // 异步执行智能体并推送 SSE
        HEARTBEAT.execute(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("STARTED")
                        .data(Map.of("message", "智能体已启动")));

                // 把 SseEmitter.send 包装成 Consumer<AgentMessage>，让工具类也能推送事件
                java.util.function.Consumer<AgentMessage> sink = msg -> {
                    try {
                        log.info("[SSE] 推送事件: type={}, toolName={}", msg.getType(), msg.getToolName());
                        emitter.send(SseEmitter.event()
                                .name(msg.getType().name())
                                .data(msg));
                    } catch (IOException e) {
                        log.warn("SSE 发送失败: {}", e.getMessage());
                        emitter.completeWithError(e);
                    }
                };

                agentExecutor.executeStream(request.getMessage(), request.getSessionId(),
                                request.getUserId(), sink)
                        .doOnNext(msg -> {
                            lastSendAt[0] = System.currentTimeMillis();
                        })
                        .doOnError(e -> {
                            log.error("智能体流式执行失败: {}", e.getMessage(), e);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("ERROR")
                                        .data(Map.of("content", "执行失败：" + e.getMessage())));
                            } catch (IOException ignore) {}
                            emitter.completeWithError(e);
                        })
                        .doFinally(signal -> {
                            try {
                                emitter.complete();
                            } catch (Exception ignore) {}
                            scheduler.shutdownNow();
                        })
                        .subscribe();

            } catch (Exception e) {
                log.error("SSE 初始化失败: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("ERROR")
                            .data(Map.of("content", "初始化失败：" + e.getMessage())));
                } catch (IOException ignore) {}
                emitter.completeWithError(e);
                scheduler.shutdownNow();
            }
        });

        return emitter;
    }

    /**
     * 轮询接口（兜底）：按 runId 获取消息增量。
     */
    @GetMapping("/run/{runId}/messages")
    public ResponseEntity<Result<List<AiMessage>>> pollMessages(@PathVariable String runId,
                                                            @RequestParam(required = false) Long sinceId) {
        List<AiMessage> msgs = messageStoreService.getByRunId(runId);
        List<AiMessage> incremental = msgs.stream()
                .filter(m -> sinceId == null || m.getId() > sinceId)
                .toList();
        return ResponseEntity.ok(Result.success(incremental));
    }
}