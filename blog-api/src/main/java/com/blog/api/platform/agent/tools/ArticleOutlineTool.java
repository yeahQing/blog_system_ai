package com.blog.api.platform.agent.tools;

import com.blog.api.platform.registry.ModelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.minimax.MiniMaxChatOptions;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具：生成文章大纲。
 * <p>调用 MiniMax 根据主题生成结构化大纲，参考检索到的相关内容避免重复。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleOutlineTool {

    private final ModelRegistry modelRegistry;

    @Tool(description = "根据文章主题生成大纲。用于写作前列出章节结构。"
            + "输入：文章主题、参考的已有内容（可为空）；输出：Markdown 大纲。")
    public String generateOutline(
            @ToolParam(description = "文章主题") String topic,
            @ToolParam(description = "参考的已有内容，避免重复；无则传空字符串") String references) {
        log.info("[Tool] generate_outline: topic={}", topic);
        ChatModel chatModel = modelRegistry.getDefaultChatModel();
        String systemPrompt = """
                你是中文技术博客编辑。请根据主题生成文章大纲。
                要求：
                - 使用简体中文，Markdown 格式
                - 5-7 个章节，每章带简要说明
                - 参考已有内容避免重复，突出新颖角度
                - 只输出大纲，不要额外说明
                """;
        String userPrompt = "文章主题：" + topic + "\n\n参考的已有内容：\n" +
                (references == null || references.isBlank() ? "（无）" : references);
        Prompt prompt = new Prompt(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                MiniMaxChatOptions.builder().maxTokens(1024).temperature(0.5).build());
        return chatModel.call(prompt).getResult().getOutput().getText();
    }
}
