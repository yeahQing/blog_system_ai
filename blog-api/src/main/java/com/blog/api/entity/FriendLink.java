package com.blog.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * 前台「常用工具」页展示的链接（数据表 blog_friend_link 名称沿用历史实现）。
 */
@Entity
@Table(name = "blog_friend_link")
public class FriendLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 512)
    private String url;

    /** 前台卡片上的简短说明 */
    @Column(length = 255)
    private String description;

    @Builder.Default
    private Integer sortOrder = 0;

    @Builder.Default
    private Boolean enabled = true;
}
