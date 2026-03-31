package com.blog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticleCoverConfirmRequest {

    /**
     * 预览接口返回的 Base64；也支持 data:image/jpeg;base64, 整段粘贴。
     */
    @NotBlank(message = "图片数据不能为空")
    @Size(max = 12_000_000)
    private String imageBase64;
}
