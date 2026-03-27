package com.blog.api.service;

import com.blog.api.dto.CommentCreateRequest;
import com.blog.api.entity.Comment;
import com.blog.api.repository.ArticleRepository;
import com.blog.api.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;

    public List<Comment> listByArticle(Long articleId) {
        return commentRepository.findByArticleIdAndApprovedTrueOrderByCreatedAtAsc(articleId);
    }

    @Transactional
    public Comment create(CommentCreateRequest req) {
        if (req.getArticleId() == null) {
            throw new IllegalArgumentException("articleId 必填");
        }
        if (req.getNickname() == null || req.getNickname().isBlank()) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new IllegalArgumentException("内容不能为空");
        }
        articleRepository.findByIdAndIsDraftFalseAndIsDeletedFalse(req.getArticleId())
                .orElseThrow(() -> new IllegalArgumentException("文章不存在或未发布"));

        if (req.getParentId() != null) {
            Comment parent = commentRepository.findById(req.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("父评论不存在"));
            if (!parent.getArticleId().equals(req.getArticleId())) {
                throw new IllegalArgumentException("父评论不属于该文章");
            }
        }

        String nick = req.getNickname().trim();
        if (nick.length() > 64) {
            nick = nick.substring(0, 64);
        }
        String content = req.getContent().trim();
        if (content.length() > 5000) {
            content = content.substring(0, 5000);
        }

        Comment c = Comment.builder()
                .articleId(req.getArticleId())
                .parentId(req.getParentId())
                .nickname(nick)
                .email(req.getEmail() != null ? req.getEmail().trim() : null)
                .content(content)
                .approved(true)
                .build();
        return commentRepository.save(c);
    }

    @Transactional
    public boolean like(Long id) {
        return commentRepository.incrementLike(id) > 0;
    }
}
