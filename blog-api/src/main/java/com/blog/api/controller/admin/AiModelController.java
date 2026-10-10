package com.blog.api.controller.admin;

import com.blog.api.common.Result;
import com.blog.api.platform.model.AiModel;
import com.blog.api.platform.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型管理接口（AI 控制台）。
 * <p>API Key 返回时做掩码处理（不返回明文）。</p>
 */
@RestController
@RequestMapping("/api/admin/platform/models")
@RequiredArgsConstructor
public class AiModelController {

    private final ModelService modelService;

    @GetMapping
    public ResponseEntity<Result<List<AiModel>>> list(@RequestParam(required = false) String type) {
        List<AiModel> list = type != null ? modelService.listByType(type) : modelService.listAll();
        list.forEach(this::maskApiKey);
        return ResponseEntity.ok(Result.success(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<AiModel>> get(@PathVariable Long id) {
        AiModel m = modelService.getById(id);
        maskApiKey(m);
        return ResponseEntity.ok(Result.success(m));
    }

    @PostMapping
    public ResponseEntity<Result<AiModel>> create(@RequestBody AiModel model) {
        AiModel saved = modelService.create(model);
        maskApiKey(saved);
        return ResponseEntity.ok(Result.success(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<AiModel>> update(@PathVariable Long id, @RequestBody AiModel form) {
        AiModel updated = modelService.update(id, form);
        maskApiKey(updated);
        return ResponseEntity.ok(Result.success(updated));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<Result<AiModel>> setDefault(@PathVariable Long id) {
        AiModel m = modelService.setDefault(id);
        maskApiKey(m);
        return ResponseEntity.ok(Result.success(m));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> delete(@PathVariable Long id) {
        modelService.delete(id);
        return ResponseEntity.ok(Result.success());
    }

    /** API Key 掩码：仅保留是否存在标记，不返回明文 */
    private void maskApiKey(AiModel m) {
        if (m.getApiKeyEncrypted() != null && !m.getApiKeyEncrypted().isBlank()) {
            m.setApiKeyEncrypted("********");
        }
    }
}
