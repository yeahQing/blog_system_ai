package com.blog.api.controller;

import com.blog.api.common.Result;
import com.blog.api.entity.Tag;
import com.blog.api.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/tags")
@RequiredArgsConstructor
public class PublicTagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<Result<List<Tag>>> list() {
        return ResponseEntity.ok(Result.success(tagService.listAll()));
    }
}
