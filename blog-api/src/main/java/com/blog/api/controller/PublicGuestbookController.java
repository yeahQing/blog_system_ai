package com.blog.api.controller;

import com.blog.api.common.Result;
import com.blog.api.dto.GuestbookCreateRequest;
import com.blog.api.entity.GuestbookMessage;
import com.blog.api.service.GuestbookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/guestbook")
@RequiredArgsConstructor
public class PublicGuestbookController {

    private final GuestbookService guestbookService;

    @GetMapping
    public ResponseEntity<Result<Page<GuestbookMessage>>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(Result.success(guestbookService.list(pageable)));
    }

    @PostMapping
    public ResponseEntity<Result<GuestbookMessage>> create(@RequestBody GuestbookCreateRequest body) {
        return ResponseEntity.ok(Result.success(guestbookService.create(body)));
    }
}
