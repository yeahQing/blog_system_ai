package com.blog.api.service;

import com.blog.api.dto.DashboardStatsResponse;
import com.blog.api.repository.ArticleRepository;
import com.blog.api.repository.CategoryRepository;
import com.blog.api.repository.CommentRepository;
import com.blog.api.repository.FriendLinkRepository;
import com.blog.api.repository.GuestbookRepository;
import com.blog.api.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardStatsService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CommentRepository commentRepository;
    private final GuestbookRepository guestbookRepository;
    private final FriendLinkRepository friendLinkRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse stats() {
        long total = articleRepository.countByIsDeletedFalse();
        long drafts = articleRepository.countByIsDeletedFalseAndIsDraft(true);
        long published = articleRepository.countByIsDeletedFalseAndIsDraft(false);
        long views = articleRepository.sumViewsNotDeleted();
        return DashboardStatsResponse.builder()
                .articleTotal(total)
                .articlePublished(published)
                .articleDrafts(drafts)
                .totalViews(views)
                .categories(categoryRepository.count())
                .tags(tagRepository.count())
                .comments(commentRepository.count())
                .guestbookMessages(guestbookRepository.count())
                .friendLinksTotal(friendLinkRepository.count())
                .friendLinksEnabled(friendLinkRepository.countByEnabledTrue())
                .build();
    }
}
