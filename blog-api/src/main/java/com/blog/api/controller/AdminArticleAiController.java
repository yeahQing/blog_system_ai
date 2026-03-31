package com.blog.api.controller;

import com.blog.api.common.Result;
import com.blog.api.dto.ArticleAiAssistRequest;
import com.blog.api.dto.ArticleCoverConfirmRequest;
import com.blog.api.dto.ArticleCoverGenerateRequest;
import com.blog.api.dto.ArticleCoverGenerateResponse;
import com.blog.api.dto.ArticleCoverPreviewResponse;
import com.blog.api.dto.ArticleAiSummaryResponse;
import com.blog.api.dto.ArticleAiTitlesResponse;
import com.blog.api.service.ArticleAiService;
import com.blog.api.service.ArticleCoverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/article/ai")
@RequiredArgsConstructor
public class AdminArticleAiController {

    private final ArticleAiService articleAiService;
    private final ArticleCoverService articleCoverService;

    @PostMapping("/summary")
    public ResponseEntity<Result<ArticleAiSummaryResponse>> summary(@Valid @RequestBody ArticleAiAssistRequest request) {
        return ResponseEntity.ok(Result.success(articleAiService.generateSummary(request)));
    }

    @PostMapping("/titles")
    public ResponseEntity<Result<ArticleAiTitlesResponse>> titles(@Valid @RequestBody ArticleAiAssistRequest request) {
        return ResponseEntity.ok(Result.success(articleAiService.suggestTitles(request)));
    }

    /** MiniMax 文生图，仅返回 Base64 预览（不上传图床） */
    @PostMapping("/cover/preview")
    public ResponseEntity<Result<ArticleCoverPreviewResponse>> coverPreview(@Valid @RequestBody ArticleCoverGenerateRequest request) {
        return ResponseEntity.ok(Result.success(articleCoverService.generateCoverPreview(request)));
    }

    /** 将预览图上传到闪电图床，返回可保存的封面 URL */
    @PostMapping("/cover/confirm")
    public ResponseEntity<Result<ArticleCoverGenerateResponse>> coverConfirm(@Valid @RequestBody ArticleCoverConfirmRequest request) {
        return ResponseEntity.ok(Result.success(articleCoverService.confirmCoverUpload(request)));
    }
}
