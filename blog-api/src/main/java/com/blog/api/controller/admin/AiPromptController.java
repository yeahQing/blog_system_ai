package com.blog.api.controller.admin;

import com.blog.api.common.Result;
import com.blog.api.platform.model.AiPrompt;
import com.blog.api.platform.service.PromptService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提示词管理接口（AI 控制台）。
 */
@RestController
@RequestMapping("/api/admin/platform/prompts")
@RequiredArgsConstructor
public class AiPromptController {

    private final PromptService promptService;

    /** 获取某 code 的当前生效版本 */
    @GetMapping("/active")
    public ResponseEntity<Result<AiPrompt>> getActive(@RequestParam String code) {
        return ResponseEntity.ok(Result.success(promptService.getActive(code).orElse(null)));
    }

    /** 查询某 code 的所有版本历史 */
    @GetMapping("/versions")
    public ResponseEntity<Result<List<AiPrompt>>> getVersions(@RequestParam String code) {
        return ResponseEntity.ok(Result.success(promptService.getVersions(code)));
    }

    /** 保存为新版本 */
    @PostMapping
    public ResponseEntity<Result<AiPrompt>> saveNewVersion(@RequestBody SavePromptRequest req) {
        AiPrompt p = promptService.saveNewVersion(
                req.getCode(), req.getName(), req.getDescription(), req.getType(),
                req.getContent(), req.getVariables());
        return ResponseEntity.ok(Result.success(p));
    }

    /** 回滚到指定版本 */
    @PutMapping("/rollback/{promptId}")
    public ResponseEntity<Result<AiPrompt>> rollback(@PathVariable Long promptId) {
        return ResponseEntity.ok(Result.success(promptService.rollback(promptId)));
    }

    @Data
    public static class SavePromptRequest {
        private String code;
        private String name;
        private String description;
        private String type;
        private String content;
        private String variables;
    }
}
