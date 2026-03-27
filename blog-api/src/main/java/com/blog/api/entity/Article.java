package com.blog.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 文章实体类 (支持 Markdown)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "blog_article")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;      // 标题

    @Column(columnDefinition = "TEXT")
    private String content;    // Markdown 原始内容

    @Column(columnDefinition = "TEXT")
    private String htmlContent; // 渲染后的 HTML 内容 (用于缓存或前台快速展示)

    private String summary;    // 摘要

    private String cover;      // 封面图 URL

    @Builder.Default
    private Integer views = 0; // 阅读量

    @Builder.Default
    private Integer likeCount = 0; // 点赞数

    @Builder.Default
    private Boolean isDraft = true; // 是否为草稿

    @Builder.Default
    private Boolean isDeleted = false; // 是否逻辑删除

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category; // 文章分类

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "blog_article_tag",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();
}
