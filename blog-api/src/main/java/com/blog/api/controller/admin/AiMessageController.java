package com.blog.api.controller.admin;

import com.blog.api.common.Result;
import com.blog.api.platform.model.AiMessage;
import com.blog.api.platform.service.MessageStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息流可视化接口（AI 控制台）。
 */
@RestController
@RequestMapping("/api/admin/platform/messages")
@RequiredArgsConstructor
public class AiMessageController {

    private final MessageStoreService messageStoreService;
    private final JdbcTemplate jdbcTemplate;

    /** 列出所有会话（去重 session_id，含消息数和最近活跃时间，按时间倒序） */
    @GetMapping("/sessions")
    public ResponseEntity<Result<List<Map<String, Object>>>> listSessions() {
        String sql = """
                SELECT session_id, COUNT(*) AS msg_count, MAX(created_at) AS last_active
                FROM ai_message
                GROUP BY session_id
                ORDER BY last_active DESC
                LIMIT 100
                """;
        List<Map<String, Object>> sessions = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(Result.success(sessions));
    }

    /** 按会话查询完整消息流（时间升序） */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Result<List<AiMessage>>> getBySession(@PathVariable String sessionId,
                                                                @RequestParam(required = false) String role) {
        List<AiMessage> msgs = role != null
                ? messageStoreService.getBySessionAndRole(sessionId, role)
                : messageStoreService.getBySession(sessionId);
        return ResponseEntity.ok(Result.success(msgs));
    }

    /** 按执行 ID 查询单次执行轨迹 */
    @GetMapping("/run/{runId}")
    public ResponseEntity<Result<List<AiMessage>>> getByRunId(@PathVariable String runId) {
        return ResponseEntity.ok(Result.success(messageStoreService.getByRunId(runId)));
    }

    /** 按会话分页查询（最新优先） */
    @GetMapping("/session/{sessionId}/paged")
    public ResponseEntity<Result<Page<AiMessage>>> getPaged(@PathVariable String sessionId,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(Result.success(messageStoreService.getBySessionPaged(sessionId, pageable)));
    }
}
