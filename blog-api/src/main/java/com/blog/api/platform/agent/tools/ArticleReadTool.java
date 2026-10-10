package com.blog.api.platform.agent.tools;

import com.blog.api.entity.Article;
import com.blog.api.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 工具：读取文章详情。
 * <p>智能体调用此工具获取指定文章的完整内容，用于改文章、参考已有文章结构。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleReadTool {

    private final ArticleRepository articleRepository;

    @Tool(description = "根据文章 ID 读取博客文章详情（标题、摘要、正文）。用于查看或参考已有文章。"
            + "输入：文章 ID；输出：文章完整内容。")
    public String getArticleDetail(
            @ToolParam(description = "文章 ID") Long articleId) {
        log.info("[Tool] get_article_detail: id={}", articleId);
        Optional<Article> opt = articleRepository.findById(articleId);
        if (opt.isEmpty()) {
            return "文章不存在: id=" + articleId;
        }
        Article a = opt.get();
        StringBuilder sb = new StringBuilder();
        sb.append("标题：").append(a.getTitle()).append("\n");
        if (a.getSummary() != null && !a.getSummary().isBlank()) {
            sb.append("摘要：").append(a.getSummary()).append("\n");
        }
        sb.append("正文：\n").append(a.getContent() != null ? a.getContent() : "(空)");
        // 截断超长正文，避免超出模型上下文
        String result = sb.toString();
        if (result.length() > 6000) {
            result = result.substring(0, 6000) + "\n…（正文已截断，仅展示前 6000 字）";
        }
        return result;
    }
}
