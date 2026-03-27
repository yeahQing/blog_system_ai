package com.blog.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 后台侧边菜单显示配置（单例行 id=1）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "blog_admin_menu_visibility")
public class AdminMenuVisibility {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String configJson;
}
