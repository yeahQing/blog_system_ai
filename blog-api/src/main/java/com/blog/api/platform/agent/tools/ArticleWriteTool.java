package com.blog.api.platform.agent.tools;

import com.blog.api.platform.agent.AgentSseSinkRegistry;
import com.blog.api.platform.rag.ChunkSearchHit;
import com.blog.api.platform.rag.RagService;
import com.blog.api.platform.registry.ModelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.minimax.MiniMaxChatOptions;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具：RAG 增强写作单个章节。
 * <p>写作前先检索历史文章相关内容作为参考，拼装进 Prompt，确保风格一致且复用真实事实。
 * 这是 RAG pipeline 的 ③ 生成阶段。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleWriteTool {

    private final ModelRegistry modelRegistry;
    private final RagService ragService;

    @Tool(description = "根据大纲写一个章节。会自动检索历史文章中相关内容作为参考，"
            + "确保风格一致且不重复已有内容。输入：文章主题、章节标题、已写章节（保持连贯）。"
            + "输出：章节 Markdown 正文。")
    public String writeSection(
            @ToolParam(description = "文章主题") String articleTopic,
            @ToolParam(description = "本章节标题") String sectionTitle,
            @ToolParam(description = "已写章节内容，用于保持连贯；首次写作传空字符串") String previousSections) {
        log.info("[Tool] write_section: topic={}, section={}", articleTopic, sectionTitle);

        // ① 检索：用「主题 + 章节标题」拼接查询
        String query = articleTopic + " " + sectionTitle;
        List<ChunkSearchHit> hits = ragService.retrieve(query, 5);

        // ② 增强：拼装上下文
        String context = ragService.buildContext(hits);

        // ③ 生成：带检索上下文的 Prompt
        ChatModel chatModel = modelRegistry.getDefaultChatModel();
        String systemPrompt = """
                你正在写一篇技术博客。以下是博客中已有的相关内容，请参考其风格和事实，但不要直接复制：

                【检索到的相关内容】
                %s

                请写本章节正文，要求：
                - 简体中文，Markdown 格式
                - 风格与检索到的内容保持一致
                - 如果检索内容中有可复用的事实/命令，优先使用，不要编造替代品
                - 与已写章节保持连贯，不重复
                """.formatted(context);

        String userPrompt = "【文章主题】" + articleTopic + "\n【本章节标题】" + sectionTitle
                + "\n【已写章节】\n" + (previousSections == null || previousSections.isBlank()
                        ? "（无，这是第一章）"
                        : truncatePrevious(previousSections));

        Prompt prompt = new Prompt(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                MiniMaxChatOptions.builder().maxTokens(2048).temperature(0.5).build());
        String content = chatModel.call(prompt).getResult().getOutput().getText();
        log.info("[Tool] write_section 完成: section={}, 字数={}", sectionTitle, content.length());
        // 推送章节内容给前端（实时展示写作进度）
        AgentSseSinkRegistry.notifyToolResult("write_section", "✍️ 已写章节：" + sectionTitle + "\n\n" + content);
        return content;
    }

    /**
     * 截短已写章节内容，避免工具参数 JSON 过大导致 MiniMax 返回的 arguments 被截断。
     * 只保留最近 800 字（含最后 2-3 章的概要），保留章节标题用于风格连贯。
     */
    static String truncatePrevious(String prev) {
        if (prev == null || prev.isBlank()) {
            return "（无，这是第一章）";
        }
        if (prev.length() <= 800) {
            return prev;
        }
        // 保留最近 800 字，从最近的 ## 标题处截断更连贯
        String tail = prev.substring(prev.length() - 800);
        int headingIdx = tail.indexOf("\n## ");
        if (headingIdx > 0 && headingIdx < 200) {
            tail = tail.substring(headingIdx + 1);
        }
        return "...（前文已省略）...\n" + tail;
    }
}
