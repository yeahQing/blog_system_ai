package com.blog.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "blog.site")
public class BlogSiteProperties {

    /** 前台导航栏左侧站点名（可被数据库覆盖） */
    private String navTitle = "AI BLOG";
    /** 导航 Logo 图片 URL，空则前台用默认图标 */
    private String navLogoUrl = "";

    /** 首页横幅主标题 */
    private String heroTitle = "探索代码与生活";
    /** 首页横幅副标题 */
    private String heroSubtitle = "分享技术洞见，记录成长瞬间";

    private String authorName = "博主";
    private String authorBio = "记录技术、阅读与生活。";
    private String authorAvatar = "https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png";

    /**
     * 页脚一行文案；可使用占位符 {year} 表示当前年份。
     */
    private String footerText = "© {year} AI BLOG · Powered by Vue 3 & Spring Boot";
}
