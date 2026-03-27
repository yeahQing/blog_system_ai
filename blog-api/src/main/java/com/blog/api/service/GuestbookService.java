package com.blog.api.service;

import com.blog.api.dto.GuestbookCreateRequest;
import com.blog.api.entity.GuestbookMessage;
import com.blog.api.repository.GuestbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuestbookService {

    private final GuestbookRepository guestbookRepository;

    public Page<GuestbookMessage> list(Pageable pageable) {
        return guestbookRepository.findByApprovedTrueOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public GuestbookMessage create(GuestbookCreateRequest req) {
        if (req.getNickname() == null || req.getNickname().isBlank()) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new IllegalArgumentException("留言内容不能为空");
        }
        String content = req.getContent().trim();
        if (content.length() > 2000) {
            content = content.substring(0, 2000);
        }
        GuestbookMessage m = GuestbookMessage.builder()
                .nickname(req.getNickname().trim())
                .email(req.getEmail() != null ? req.getEmail().trim() : null)
                .content(content)
                .approved(true)
                .build();
        return guestbookRepository.save(m);
    }
}
