package com.blog.api.controller;

import com.blog.api.common.Result;
import com.blog.api.entity.FriendLink;
import com.blog.api.service.FriendLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/friend-links")
@RequiredArgsConstructor
public class PublicFriendLinkController {

    private final FriendLinkService friendLinkService;

    @GetMapping
    public ResponseEntity<Result<List<FriendLink>>> list() {
        return ResponseEntity.ok(Result.success(friendLinkService.listEnabled()));
    }
}
