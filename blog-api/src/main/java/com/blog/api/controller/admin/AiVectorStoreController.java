package com.blog.api.controller.admin;

import com.blog.api.common.Result;
import com.blog.api.platform.model.AiVectorStore;
import com.blog.api.platform.registry.ModelRegistry;
import com.blog.api.platform.service.VectorStoreService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 向量库管理接口（AI 控制台）。
 */
@RestController
@RequestMapping("/api/admin/platform/vector-stores")
@RequiredArgsConstructor
public class AiVectorStoreController {

    private final VectorStoreService vectorStoreService;
    private final ModelRegistry modelRegistry;

    @GetMapping
    public ResponseEntity<Result<List<AiVectorStore>>> list(@RequestParam(defaultValue = "false") boolean all) {
        return ResponseEntity.ok(Result.success(all ? vectorStoreService.listAll() : vectorStoreService.listEnabled()));
    }

    @GetMapping("/{code}")
    public ResponseEntity<Result<AiVectorStore>> get(@PathVariable String code) {
        return ResponseEntity.ok(Result.success(vectorStoreService.getByCode(code).orElse(null)));
    }

    /** 新建 Collection（注册元数据 + 动态建物理表） */
    @PostMapping
    public ResponseEntity<Result<AiVectorStore>> create(@RequestBody CreateVectorStoreRequest req) {
        AiVectorStore s = vectorStoreService.createCollection(
                req.getCode(), req.getName(), req.getBusinessType(),
                req.getEmbeddingModelId(), req.getChunkStrategy(),
                req.getChunkMaxChars(), req.getDescription());
        return ResponseEntity.ok(Result.success(s));
    }

    /** 启用/禁用 Collection */
    @PutMapping("/{id}/enabled")
    public ResponseEntity<Result<AiVectorStore>> toggleEnabled(@PathVariable Long id,
                                                               @RequestParam boolean enabled) {
        return ResponseEntity.ok(Result.success(vectorStoreService.toggleEnabled(id, enabled)));
    }

    /**
     * Embedding 连通性测试（走 ModelRegistry → OllamaEmbeddingClient 全链路）。
     * <p>临时接口，验证默认 EMBEDDING 模型可正常向量化。</p>
     */
    @PostMapping("/embed-test")
    public ResponseEntity<Result<EmbedTestResponse>> embedTest(@RequestParam String text) {
        float[] vec = modelRegistry.embedWithDefault(text);
        return ResponseEntity.ok(Result.success(new EmbedTestResponse(vec.length, vec)));
    }

    @Data
    public static class CreateVectorStoreRequest {
        private String code;
        private String name;
        private String businessType;
        private Long embeddingModelId;
        private String chunkStrategy;
        private Integer chunkMaxChars;
        private String description;
    }

    @Data
    @AllArgsConstructor
    public static class EmbedTestResponse {
        private int dimension;
        private float[] embedding;
    }
}
