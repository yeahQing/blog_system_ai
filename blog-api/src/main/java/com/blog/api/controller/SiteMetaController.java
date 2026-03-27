package com.blog.api.controller;

import com.blog.api.common.Result;
import com.blog.api.dto.SiteBrandingResolved;
import com.blog.api.service.SiteBrandingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public/site")
@RequiredArgsConstructor
public class SiteMetaController {

    private final SiteBrandingService siteBrandingService;

    /**
     * 前台完整站点配置（导航 + 博主信息）
     */
    @GetMapping("/config")
    public ResponseEntity<Result<SiteBrandingResolved>> config() {
        return ResponseEntity.ok(Result.success(siteBrandingService.resolvePublic()));
    }

    /**
     * 兼容旧接口：仅博主三字段
     */
    @GetMapping("/author")
    public ResponseEntity<Result<Map<String, String>>> author() {
        SiteBrandingResolved r = siteBrandingService.resolvePublic();
        return ResponseEntity.ok(Result.success(Map.of(
                "name", r.getAuthorName(),
                "bio", r.getAuthorBio(),
                "avatar", r.getAuthorAvatar()
        )));
    }
}
