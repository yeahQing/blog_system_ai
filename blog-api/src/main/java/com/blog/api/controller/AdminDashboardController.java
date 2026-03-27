package com.blog.api.controller;

import com.blog.api.common.Result;
import com.blog.api.dto.DashboardStatsResponse;
import com.blog.api.service.DashboardStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardStatsService dashboardStatsService;

    @GetMapping("/stats")
    public ResponseEntity<Result<DashboardStatsResponse>> stats() {
        return ResponseEntity.ok(Result.success(dashboardStatsService.stats()));
    }
}
