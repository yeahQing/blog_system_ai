package com.blog.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCoverPreviewResponse {

    /** 不含 data: 前缀的 Base64，供 img src 拼接或确认上传回传 */
    private String imageBase64;

    /** 如 image/jpeg，用于前端 data URL */
    private String mimeType;
}
