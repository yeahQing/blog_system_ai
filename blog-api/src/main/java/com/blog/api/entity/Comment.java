package com.blog.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 文章评论（支持楼中楼：parentId 指向父评论）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "blog_comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long articleId;

    /** 一级评论为 null；回复时指向父评论 id */
    private Long parentId;

    @Column(nullable = false, length = 64)
    private String nickname;

    @Column(length = 120)
    private String email;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    private Integer likeCount = 0;

    /** 后台审核：false 时前台列表不展示 */
    @Builder.Default
    private Boolean approved = true;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
