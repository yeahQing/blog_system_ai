package com.blog.api.dto;

import lombok.Data;

import java.util.List;

/**
 * 文章保存请求体（管理端）。
 * <p>
 * 使用 {@code tagIds} / {@code categoryId} 显式传参，避免将 JPA 实体作为 JSON 根对象反序列化时
 * {@code tags} 集合绑定不稳定导致关联未写入 {@code blog_article_tag}。
 */
@Data
public class ArticleSaveRequest {

    private Long id;
    private String title;
    private String content;
    private String htmlContent;
    private String summary;
    private String cover;
    private Boolean isDraft;
    private Boolean isDeleted;

    /** 是否置顶（前台列表优先） */
    private Boolean pinned;

    /** 分类主键，null 表示不选分类 */
    private Long categoryId;

    /**
     * 标签 id 列表。更新时：null 表示本次不修改标签关联；空列表表示清空。
     * 新建时 null 视为无标签。
     */
    private List<Long> tagIds;
}
