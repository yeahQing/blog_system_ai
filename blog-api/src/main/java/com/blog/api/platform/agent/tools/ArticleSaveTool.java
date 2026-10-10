package com.blog.api.platform.agent.tools;

import com.blog.api.dto.ArticleSaveRequest;
import com.blog.api.entity.Article;
import com.blog.api.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 工具：保存文章草稿。
 * <p>智能体完成写作后调用此工具保存，标记 isDraft=true（需人工确认发布）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleSaveTool {

    private final ArticleService articleService;

    @Tool(description = "将生成的文章保存为草稿（isDraft=true，需人工确认后发布）。"
            + "输入：标题、正文（Markdown）、摘要；输出：保存后的文章 ID。")
    public String saveDraft(
            @ToolParam(description = "文章标题") String title,
            @ToolParam(description = "文章正文，Markdown 格式") String content,
            @ToolParam(description = "文章摘要，不超过 220 字") String summary) {
        log.info("[Tool] save_draft: title={}", title);
        ArticleSaveRequest req = new ArticleSaveRequest();
        req.setTitle(title);
        req.setContent(content);
        req.setSummary(summary);
        req.setIsDraft(true);
        Article saved = articleService.saveArticle(req);
        String result = "草稿已保存，文章 ID=" + saved.getId() + "，标题《" + saved.getTitle() + "》";
        log.info("[Tool] save_draft 完成: id={}", saved.getId());
        return result;
    }
}
