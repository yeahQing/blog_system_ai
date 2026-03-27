package com.blog.api.dto;

import lombok.Data;

@Data
public class GuestbookCreateRequest {

    private String nickname;
    private String email;
    private String content;
}
