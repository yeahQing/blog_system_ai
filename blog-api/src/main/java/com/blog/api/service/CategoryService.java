package com.blog.api.service;

import com.blog.api.entity.Category;
import java.util.List;

/**
 * 分类业务接口
 */
public interface CategoryService {
    List<Category> getAllCategories();
    Category saveCategory(Category category);
    void deleteCategory(Long id);
}
