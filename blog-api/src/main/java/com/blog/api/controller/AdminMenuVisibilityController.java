package com.blog.api.controller;

import com.blog.api.common.Result;
import com.blog.api.service.AdminMenuVisibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/menu-visibility")
@RequiredArgsConstructor
@Tag(name = "后台菜单", description = "侧边栏显示配置")
public class AdminMenuVisibilityController {

    private final AdminMenuVisibilityService adminMenuVisibilityService;

    @Operation(summary = "获取菜单显示配置")
    @GetMapping
    public ResponseEntity<Result<Map<String, Boolean>>> get() {
        return ResponseEntity.ok(Result.success(adminMenuVisibilityService.getResolved()));
    }

    @Operation(summary = "保存菜单显示配置")
    @PutMapping
    public ResponseEntity<Result<Map<String, Boolean>>> update(@RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(Result.success(adminMenuVisibilityService.save(body)));
    }
}
