package com.blog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticleAiAssistRequest {

    @Size(max = 500)
    private String title;

    @NotBlank(message = "正文不能为空")
    @Size(max = 100_000, message = "正文过长")
    private String content;
}
