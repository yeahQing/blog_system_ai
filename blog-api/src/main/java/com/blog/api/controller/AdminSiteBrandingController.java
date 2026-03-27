package com.blog.api.controller;

import com.blog.api.common.Result;
import com.blog.api.dto.SiteBrandingResolved;
import com.blog.api.dto.SiteBrandingUpdateRequest;
import com.blog.api.service.SiteBrandingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/site-branding")
@RequiredArgsConstructor
public class AdminSiteBrandingController {

    private final SiteBrandingService siteBrandingService;

    @GetMapping
    public ResponseEntity<Result<SiteBrandingResolved>> get() {
        return ResponseEntity.ok(Result.success(siteBrandingService.getForAdmin()));
    }

    @PutMapping
    public ResponseEntity<Result<SiteBrandingResolved>> update(@RequestBody SiteBrandingUpdateRequest body) {
        siteBrandingService.save(body);
        return ResponseEntity.ok(Result.success(siteBrandingService.getForAdmin()));
    }
}
