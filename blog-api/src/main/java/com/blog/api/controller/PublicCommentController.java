package com.blog.api.controller;

import com.blog.api.common.Result;
import com.blog.api.dto.CommentCreateRequest;
import com.blog.api.entity.Comment;
import com.blog.api.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/comments")
@RequiredArgsConstructor
public class PublicCommentController {

    private final CommentService commentService;

    @GetMapping("/article/{articleId:\\d+}")
    public ResponseEntity<Result<List<Comment>>> byArticle(@PathVariable Long articleId) {
        return ResponseEntity.ok(Result.success(commentService.listByArticle(articleId)));
    }

    @PostMapping
    public ResponseEntity<Result<Comment>> create(@RequestBody CommentCreateRequest body) {
        return ResponseEntity.ok(Result.success(commentService.create(body)));
    }

    @PostMapping("/{id:\\d+}/like")
    public ResponseEntity<Result<Void>> like(@PathVariable Long id) {
        if (!commentService.like(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Result.success());
    }
}
