package com.blog.api.platform.agent.tools;

import com.blog.api.platform.agent.AgentSseSinkRegistry;
import com.blog.api.platform.rag.ChunkSearchHit;
import com.blog.api.platform.rag.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工具：语义搜索历史文章。
 * <p>智能体调用此工具检索博客中与关键词语义相关的文章，用于查文章、避免内容重复、参考写作风格。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleSearchTool {

    private final RagService ragService;

    @Tool(description = "根据关键词语义搜索博客历史文章。用于查找相关文章、避免内容重复、参考已有内容的风格。"
            + "输入：搜索关键词；输出：相关文章的标题、摘要和相似度分数。")
    public String searchArticles(
            @ToolParam(description = "搜索关键词，例如「Docker部署」「Redis缓存」") String query) {
        log.info("[Tool] search_articles: query={}", query);
        AgentSseSinkRegistry.notifyToolCall("search_articles", "关键词：" + query);
        List<ChunkSearchHit> hits = ragService.retrieve(query, 5);
        String result;
        if (hits.isEmpty()) {
            result = "未找到相关文章。";
        } else {
            result = hits.stream()
                    .map(h -> "- 《" + h.getArticleTitle() + "》（相似度:" + String.format("%.2f", h.getScore()) + "）\n  "
                            + h.getContent().replaceAll("\\s+", " ").substring(0, Math.min(80, h.getContent().length())))
                    .collect(Collectors.joining("\n\n"));
        }
        AgentSseSinkRegistry.notifyToolResult("search_articles", hits.isEmpty() ? "未找到相关文章" : "找到 " + hits.size() + " 篇相关文章");
        return result;
    }
}
