package com.blog.api.platform.rag;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文本分块结果。
 * <p>一篇文章被切分为多个 {@link Chunk}，每块带类型和顺序索引。</p>
 */
@Data
@AllArgsConstructor
public class Chunk {
    /** 块在文章中的顺序（0,1,2...） */
    private int index;
    /** 块类型：TITLE / SUMMARY / SECTION / CODE */
    private String type;
    /** 块原文 */
    private String content;
}
