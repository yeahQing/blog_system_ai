package com.blog.api.platform.agent.tools;

import com.blog.api.dto.ArticleAiAssistRequest;
import com.blog.api.dto.ArticleAiSummaryResponse;
import com.blog.api.service.ArticleAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 工具：生成文章摘要。
 * <p>复用 {@link ArticleAiService#generateSummary}，保持与现有功能一致。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleSummaryTool {

    private final ArticleAiService articleAiService;

    @Tool(description = "根据文章正文生成摘要（不超过 220 字）。用于文章列表页展示。"
            + "输入：文章正文；输出：摘要文本。")
    public String generateSummary(
            @ToolParam(description = "文章正文") String content) {
        log.info("[Tool] generate_summary: contentLength={}", content.length());
        ArticleAiAssistRequest req = new ArticleAiAssistRequest();
        req.setContent(content);
        ArticleAiSummaryResponse resp = articleAiService.generateSummary(req);
        return resp.getSummary();
    }
}
