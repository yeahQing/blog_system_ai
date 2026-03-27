package com.blog.api.dto;

import lombok.Data;

@Data
public class SiteBrandingUpdateRequest {

    private String navTitle;
    private String navLogoUrl;
    private String heroTitle;
    private String heroSubtitle;
    private String authorName;
    private String authorBio;
    private String authorAvatar;
    private String footerText;
}
