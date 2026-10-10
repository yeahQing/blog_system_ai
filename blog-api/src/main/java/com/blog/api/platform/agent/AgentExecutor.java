package com.blog.api.platform.agent;

import com.blog.api.platform.agent.tools.ArticleCoverTool;
import com.blog.api.platform.agent.tools.ArticleOutlineTool;
import com.blog.api.platform.agent.tools.ArticleReadTool;
import com.blog.api.platform.agent.tools.ArticleSaveTool;
import com.blog.api.platform.agent.tools.ArticleSearchTool;
import com.blog.api.platform.agent.tools.ArticleSummaryTool;
import com.blog.api.platform.agent.tools.ArticleTitleTool;
import com.blog.api.platform.agent.tools.ArticleWriteTool;
import com.blog.api.platform.registry.ModelRegistry;
import com.blog.api.platform.registry.PromptRegistry;
import com.blog.api.platform.service.MemoryService;
import com.blog.api.platform.service.MessageStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 智能体执行器：ChatClient + Tools 编排核心。
 * <p>同步执行 ChatClient.call()（让 Spring AI 工具循环跑通），每个工具调用和最终回复都写入 ai_message 表（TOOL/ASSISTANT 角色），
 * 前端通过轮询 /admin/platform/messages/run/{runId} 实时获取进度。
 * 这是最稳定的方案，避免 Spring MVC + reactive + SSE 的各种输出缓冲问题。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentExecutor {

    private final ModelRegistry modelRegistry;
    private final PromptRegistry promptRegistry;
    private final MemoryService memoryService;
    private final MessageStoreService messageStoreService;

    // 8 个工具 bean（保留以便后续自研 ReAct）
    private final ArticleSearchTool searchTool;
    private final ArticleReadTool readTool;
    private final ArticleOutlineTool outlineTool;
    private final ArticleWriteTool writeTool;
    private final ArticleSummaryTool summaryTool;
    private final ArticleTitleTool titleTool;
    private final ArticleCoverTool coverTool;
    private final ArticleSaveTool saveTool;

    @Value("${agent.timeout-seconds:300}")
    private long timeoutSeconds;

    /** 同步执行，立即返回 runId（智能体在后台线程执行，前端用轮询接口查进度） */
    public AgentResult execute(String userInput, String sessionId, Long userId) {
        String currentSid = sessionId;
        if (currentSid == null || currentSid.isBlank()) {
            currentSid = "sess_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        final String runId = "run_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        final String finalSid = currentSid;
        final long start = System.currentTimeMillis();

        // 持久化 USER 消息
        messageStoreService.saveUserMessage(finalSid, runId, userInput);

        // 异步执行智能体（不阻塞接口返回）
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor =
                new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("agent-async-");
        executor.initialize();

        executor.execute(() -> {
            try {
                ChatClient client = buildClient(finalSid, userInput);
                log.info("智能体异步执行开始 sessionId={}, runId={}", finalSid, runId);

                ChatResponse response = client.prompt()
                        .user(userInput)
                        .call()
                        .chatResponse();
                String reply = response.getResult().getOutput().getText();

                int latencyMs = (int) (System.currentTimeMillis() - start);
                String modelName = modelRegistry.getDefaultChatModel().getClass().getSimpleName();
                messageStoreService.saveAssistantMessage(finalSid, runId,
                        reply != null ? reply : "", modelName, null, null, latencyMs);
                memoryService.appendTurn(finalSid, userId, userInput, reply != null ? reply : "");
                log.info("智能体异步执行完成 sessionId={}, runId={}, latency={}ms",
                        finalSid, runId, latencyMs);
            } catch (Exception e) {
                log.error("智能体异步执行失败 sessionId={}, runId={}: {}", finalSid, runId, e.getMessage(), e);
                messageStoreService.saveAssistantMessage(finalSid, runId,
                        "智能体执行失败：" + e.getMessage(), "ERROR", null, null,
                        (int) (System.currentTimeMillis() - start));
            }
        });

        return new AgentResult(finalSid, runId, "已启动，runId=" + runId, 0);
    }

    /**
     * 流式执行（SSE）—— 直接接收 controller 传来的 emitter。
     * <p>每个事件通过 emitter.send 推送给前端，ai_message 表也记录（前端轮询兜底）。
     * 用 Consumer<AgentMessage> 代替 FluxSink，避免 Reactor 中间层引入异步缓冲问题。</p>
     */
    public Flux<AgentMessage> executeStream(String userInput, String sessionId, Long userId,
                                            Consumer<AgentMessage> sink) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "sess_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        final String finalSessionId = sessionId;
        final String runId = "run_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        final long start = System.currentTimeMillis();

        messageStoreService.saveUserMessage(finalSessionId, runId, userInput);

        // 把 sink 注入 ThreadLocal（工具类里可推送）
        // sink 可能为 null（来自 controller 的 SseEmitter 流），允许 null 表示只写表不推送
        AgentSseSinkRegistry.register(finalSessionId, sink);
        AgentSseSinkRegistry.setCurrentSession(finalSessionId);

        return Flux.<AgentMessage>create(fluxSink -> {
            // 把外部 sink 桥接到 FluxSink（保持 Flux 返回类型兼容）
            java.util.function.Consumer<AgentMessage> effectiveSink = msg -> {
                if (sink != null) sink.accept(msg);
                fluxSink.next(msg);
            };
            // 重新设置（覆盖前面的占位）
            AgentSseSinkRegistry.register(finalSessionId, effectiveSink);

            StringBuilder replyBuf = new StringBuilder();
            try {
                ChatClient client = buildClient(finalSessionId, userInput);
                log.info("智能体流式执行开始 sessionId={}, runId={}", finalSessionId, runId);

                // 用 stream() 拿 MiniMax 真流式响应（逐 token），工具调用循环由 Spring AI 内部处理
                // 阻塞等待流完成（在 boundedElastic 线程，不阻塞 servlet）
                java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                java.util.concurrent.atomic.AtomicReference<Throwable> errorRef = new java.util.concurrent.atomic.AtomicReference<>();

                client.prompt()
                        .user(userInput)
                        .stream()
                        .content()
                        .subscribe(
                                chunk -> {
                                    if (chunk != null && !chunk.isEmpty()) {
                                        replyBuf.append(chunk);
                                        try {
                                            sink.accept(AgentMessage.assistant(chunk));
                                        } catch (Exception ignore) {}
                                    }
                                },
                                err -> {
                                    errorRef.set(err);
                                    latch.countDown();
                                },
                                latch::countDown
                        );

                // 等待流完成（最多 timeoutSeconds）
                if (!latch.await(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)) {
                    throw new IllegalStateException("智能体执行超时");
                }
                if (errorRef.get() != null) {
                    throw errorRef.get();
                }

                String reply = replyBuf.toString();
                int latencyMs = (int) (System.currentTimeMillis() - start);
                String modelName = modelRegistry.getDefaultChatModel().getClass().getSimpleName();
                messageStoreService.saveAssistantMessage(finalSessionId, runId,
                        reply, modelName, null, null, latencyMs);
                memoryService.appendTurn(finalSessionId, userId, userInput, reply);
                try {
                    sink.accept(AgentMessage.done("sessionId=" + finalSessionId + ",runId=" + runId + ",latency=" + latencyMs + "ms"));
                } catch (Exception ignore) {}
                log.info("智能体流式执行完成 sessionId={}, latency={}ms, finalReplyLength={}",
                        finalSessionId, latencyMs, reply.length());
            } catch (Exception e) {
                log.error("智能体流式执行失败 sessionId={}: {}", finalSessionId, e.getMessage(), e);
                try { sink.accept(AgentMessage.error("执行失败：" + e.getMessage())); } catch (Exception ignore) {}
                int latencyMs = (int) (System.currentTimeMillis() - start);
                messageStoreService.saveAssistantMessage(finalSessionId, runId,
                        "智能体执行失败：" + e.getMessage(), "ERROR", null, null, latencyMs);
            } finally {
                AgentSseSinkRegistry.clearCurrentSession();
                AgentSseSinkRegistry.unregister(finalSessionId);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 构建 ChatClient（含 System Prompt + 工具 + 历史） */
    private ChatClient buildClient(String sessionId, String userInput) {
        String systemPrompt = promptRegistry.render("agent_system", Map.of());
        List<Map<String, String>> history = memoryService.loadHistory(sessionId);
        List<Message> messages = new ArrayList<>();
        for (Map<String, String> h : history) {
            if ("user".equals(h.get("role"))) {
                messages.add(new UserMessage(h.get("content")));
            }
        }
        return ChatClient.builder(modelRegistry.getDefaultChatModel())
                .defaultSystem(systemPrompt)
                .defaultTools(searchTool, readTool, outlineTool, writeTool,
                        summaryTool, titleTool, coverTool, saveTool)
                .build();
    }

    /** 将回复切分为多个小块 */
    static List<String> splitToChunks(String text) {
        if (text == null || text.isEmpty()) return List.of();
        List<String> chunks = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String part : text.split("(?<=[。.!！?？\\n])")) {
            buf.append(part);
            if (buf.length() >= 40) {
                chunks.add(buf.toString());
                buf.setLength(0);
            }
        }
        if (buf.length() > 0) chunks.add(buf.toString());
        return chunks;
    }

    public record AgentResult(String sessionId, String runId, String reply, int latencyMs) {}
}