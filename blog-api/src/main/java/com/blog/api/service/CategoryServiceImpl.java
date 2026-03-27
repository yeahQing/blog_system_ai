package com.blog.api.service;

import com.blog.api.entity.Category;
import com.blog.api.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分类业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<Category> getAllCategories() {
        log.info("查询全部分类");
        return categoryRepository.findAll();
    }

    @Override
    @Transactional
    public Category saveCategory(Category category) {
        log.info("保存分类: {}", category.getName());
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        log.warn("删除分类, ID: {}", id);
        categoryRepository.deleteById(id);
    }
}
