package com.blog.api.platform.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识块检索命中结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkSearchHit {
    private Long chunkId;
    private Long articleId;
    private Integer chunkIndex;
    private String chunkType;
    private String content;
    /** 相似度分数 0~1（1 - 余弦距离），越大越相似 */
    private double score;
    private String articleTitle;
}
