package com.blog.api.dto;

import lombok.Data;

@Data
public class CommentCreateRequest {

    private Long articleId;
    private Long parentId;
    private String nickname;
    private String email;
    private String content;
}
