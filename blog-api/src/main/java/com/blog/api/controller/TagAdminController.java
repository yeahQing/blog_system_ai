package com.blog.api.controller;

import com.blog.api.common.Result;
import com.blog.api.entity.Tag;
import com.blog.api.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tag")
@RequiredArgsConstructor
public class TagAdminController {

    private final TagService tagService;

    @GetMapping("/list")
    public ResponseEntity<Result<List<Tag>>> list() {
        return ResponseEntity.ok(Result.success(tagService.listAll()));
    }

    @PostMapping("/save")
    public ResponseEntity<Result<Tag>> save(@RequestBody Tag tag) {
        return ResponseEntity.ok(Result.success(tagService.save(tag)));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Result<Void>> delete(@PathVariable Long id) {
        tagService.deleteById(id);
        return ResponseEntity.ok(Result.success());
    }
}
