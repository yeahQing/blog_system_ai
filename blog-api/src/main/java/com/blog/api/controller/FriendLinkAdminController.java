package com.blog.api.controller;

import com.blog.api.common.Result;
import com.blog.api.entity.FriendLink;
import com.blog.api.service.FriendLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friend-link")
@RequiredArgsConstructor
public class FriendLinkAdminController {

    private final FriendLinkService friendLinkService;

    @GetMapping("/list")
    public ResponseEntity<Result<List<FriendLink>>> listAll() {
        return ResponseEntity.ok(Result.success(friendLinkService.listAll()));
    }

    @PostMapping("/save")
    public ResponseEntity<Result<FriendLink>> save(@RequestBody FriendLink link) {
        return ResponseEntity.ok(Result.success(friendLinkService.save(link)));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Result<Void>> delete(@PathVariable Long id) {
        friendLinkService.delete(id);
        return ResponseEntity.ok(Result.success());
    }
}
