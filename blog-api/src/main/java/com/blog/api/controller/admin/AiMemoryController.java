package com.blog.api.controller.admin;

import com.blog.api.common.Result;
import com.blog.api.platform.model.AiMemory;
import com.blog.api.platform.service.MemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 记忆可视化接口（AI 控制台）。
 */
@RestController
@RequestMapping("/api/admin/platform/memories")
@RequiredArgsConstructor
public class AiMemoryController {

    private final MemoryService memoryService;

    /** 列出用户的所有会话（最近活跃优先） */
    @GetMapping
    public ResponseEntity<Result<List<AiMemory>>> list(@RequestParam Long userId) {
        return ResponseEntity.ok(Result.success(memoryService.listByUser(userId)));
    }

    /** 查询会话记忆详情 */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Result<AiMemory>> get(@PathVariable String sessionId) {
        return ResponseEntity.ok(Result.success(memoryService.getBySession(sessionId).orElse(null)));
    }

    /** 清除会话记忆 */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Result<Void>> clear(@PathVariable String sessionId) {
        memoryService.clear(sessionId);
        return ResponseEntity.ok(Result.success());
    }
}
