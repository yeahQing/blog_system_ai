package com.blog.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCoverGenerateResponse {

    /** 图床返回的可外链访问的 HTTPS 地址 */
    private String coverUrl;
}
