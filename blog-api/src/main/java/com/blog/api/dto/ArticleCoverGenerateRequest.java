package com.blog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticleCoverGenerateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 500)
    private String title;

    /** 摘要；可选，会写入文生图 prompt */
    @Size(max = 4000)
    private String summary;
}
