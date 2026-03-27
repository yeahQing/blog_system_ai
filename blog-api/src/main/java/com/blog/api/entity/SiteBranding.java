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
 * 前台站点展示配置（单例行 id=1，后台可改；空字段回退到 yml）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "blog_site_branding")
public class SiteBranding {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(length = 120)
    private String navTitle;

    @Column(length = 512)
    private String navLogoUrl;

    @Column(length = 200)
    private String heroTitle;

    @Column(length = 500)
    private String heroSubtitle;

    @Column(length = 120)
    private String authorName;

    @Column(columnDefinition = "TEXT")
    private String authorBio;

    @Column(length = 512)
    private String authorAvatar;

    /** 页脚文案，支持 {year}；空则回退 yml */
    @Column(length = 500)
    private String footerText;
}
