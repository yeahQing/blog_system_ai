package com.blog.api.controller;

import com.blog.api.common.Result;
import com.blog.api.entity.Category;
import com.blog.api.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器 (API)
 */
@Slf4j
@Tag(name = "分类管理模块", description = "提供文章分类的增删改查 API")
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取全部分类")
    @GetMapping("/list")
    public ResponseEntity<Result<List<Category>>> listCategories() {
        log.info("获取全部分类");
        return ResponseEntity.ok(Result.success(categoryService.getAllCategories()));
    }

    @Operation(summary = "保存分类 (新增/修改)")
    @PostMapping("/save")
    public ResponseEntity<Result<Category>> saveCategory(@RequestBody Category category) {
        log.info("接收到保存分类请求: {}", category.getName());
        Category saved = categoryService.saveCategory(category);
        return ResponseEntity.ok(Result.success(saved));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> deleteCategory(@PathVariable Long id) {
        log.warn("管理后台请求删除分类, ID: {}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(Result.success());
    }
}
