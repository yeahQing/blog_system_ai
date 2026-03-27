package com.blog.api.controller;

import com.blog.api.common.Result;
import com.blog.api.dto.ChangePasswordRequest;
import com.blog.api.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/account")
@RequiredArgsConstructor
@Tag(name = "后台账户", description = "修改密码等")
public class AdminAccountController {

    private final UserAccountService userAccountService;

    @Operation(summary = "修改当前登录用户密码")
    @PutMapping("/password")
    public ResponseEntity<Result<Void>> changePassword(@RequestBody ChangePasswordRequest body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userAccountService.changePassword(username, body.getCurrentPassword(), body.getNewPassword());
        return ResponseEntity.ok(Result.success());
    }
}
