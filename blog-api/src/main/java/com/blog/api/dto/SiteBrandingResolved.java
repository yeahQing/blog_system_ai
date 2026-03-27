package com.blog.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 合并 yml + 数据库后的前台站点配置 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteBrandingResolved {

    private String navTitle;
    private String navLogoUrl;
    private String heroTitle;
    private String heroSubtitle;
    private String authorName;
    private String authorBio;
    private String authorAvatar;
    private String footerText;
}
