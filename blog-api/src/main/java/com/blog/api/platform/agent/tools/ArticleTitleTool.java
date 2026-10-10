package com.blog.api.platform.agent.tools;

import com.blog.api.dto.ArticleAiAssistRequest;
import com.blog.api.dto.ArticleAiTitlesResponse;
import com.blog.api.service.ArticleAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具：推荐文章标题。
 * <p>复用 {@link ArticleAiService#suggestTitles}，返回 5 个标题建议供用户选择。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleTitleTool {

    private final ArticleAiService articleAiService;

    @Tool(description = "根据文章正文推荐 5 个标题建议。输入：文章正文；输出：标题列表。")
    public String suggestTitles(
            @ToolParam(description = "文章正文") String content) {
        log.info("[Tool] suggest_titles: contentLength={}", content.length());
        ArticleAiAssistRequest req = new ArticleAiAssistRequest();
        req.setContent(content);
        ArticleAiTitlesResponse resp = articleAiService.suggestTitles(req);
        List<String> titles = resp.getTitles();
        if (titles == null || titles.isEmpty()) {
            return "未能生成标题建议。";
        }
        StringBuilder sb = new StringBuilder("标题建议：\n");
        for (int i = 0; i < titles.size(); i++) {
            sb.append(i + 1).append(". ").append(titles.get(i)).append("\n");
        }
        return sb.toString();
    }
}
