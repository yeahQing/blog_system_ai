package com.blog.api.service;

import com.blog.api.dto.ArticleAiAssistRequest;
import com.blog.api.dto.ArticleAiSummaryResponse;
import com.blog.api.dto.ArticleAiTitlesResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.ai.minimax.MiniMaxChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleAiService {

    private static final Logger log = LoggerFactory.getLogger(ArticleAiService.class);

    private static final int MAX_CONTENT_CHARS = 24_000;
    /** 日志里打印 MiniMax 原始文本的最大长度，避免超长正文刷满控制台 */
    private static final int LOG_RAW_PREVIEW_MAX = 2000;

    private final ObjectProvider<MiniMaxChatModel> chatModelProvider;
    private final ObjectMapper objectMapper;

    public ArticleAiSummaryResponse generateSummary(ArticleAiAssistRequest req) {
        MiniMaxChatModel model = requireModel();
        String body = truncate(req.getContent());
        String titleHint = req.getTitle() != null && !req.getTitle().isBlank()
                ? "当前标题（可参考）：" + req.getTitle().trim() + "\n\n"
                : "";
        String user = titleHint + "正文：\n" + body;

        MiniMaxChatOptions options = MiniMaxChatOptions.builder()
                .maxTokens(512)
                .temperature(0.35)
                .build();

        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage("""
                                你是中文技术博客编辑。请根据用户给出的 Markdown 或纯文本正文，写一段用于文章列表页展示的摘要。
                                要求：使用简体中文；不超过 220 字；客观概括主题与要点；不编造正文中没有的信息；不要加引号或「摘要：」等前缀。"""),
                        new UserMessage(user)
                ),
                options
        );

        String text = extractText(model.call(prompt));
        String summary = text != null ? text.strip() : "";
        return new ArticleAiSummaryResponse(summary);
    }

    /**
     * 根据正文（及可选当前标题）向 MiniMax 请求「标题建议」。
     * <p>调用链：{@link MiniMaxChatModel#call} → {@link ChatResponse} → 取助手文本 →
     * {@link #parseTitleJson(String)} 解析为 {@code List<String>}（要求模型输出 JSON 数组字符串）。</p>
     */
    public ArticleAiTitlesResponse suggestTitles(ArticleAiAssistRequest req) {
        // 1) 注入的 ChatModel（未配置 MiniMax 时 requireModel 会抛 IllegalStateException）
        MiniMaxChatModel model = requireModel();
        // 2) 正文截断，避免超过模型上下文；不在日志中输出全文
        String body = truncate(req.getContent());
        String titleHint = req.getTitle() != null && !req.getTitle().isBlank()
                ? "当前标题（可改写或推翻）：" + req.getTitle().trim() + "\n\n"
                : "";
        String user = titleHint + "正文：\n" + body;

        log.info("suggestTitles: 准备调用 MiniMax, 正文字符数={}, 是否附带当前标题={}",
                body.length(), !titleHint.isEmpty());

        // 3) 本次调用的采样参数（会覆盖/合并全局 minimax chat options，视 Spring AI 行为而定）
        MiniMaxChatOptions titleOptions = MiniMaxChatOptions.builder()
                .maxTokens(512)
                .temperature(0.55)
                .build();

        // 4) Prompt = 一条 System（角色与输出格式） + 一条 User（用户侧拼接的正文）
        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage("""
                                你是中文技术博客编辑。请根据正文生成 5 个中文标题建议：简洁、贴合内容、吸引读者但避免标题党。
                                只输出一个 JSON 数组，元素为字符串，例如 ["标题1","标题2"]。不要 markdown 代码块，不要任何其他说明文字。"""),
                        new UserMessage(user)
                ),
                titleOptions
        );

        // 5) HTTP 调用 MiniMax，响应封装为 ChatResponse；extractText 只取助手回复的纯文本
        ChatResponse chatResponse = model.call(prompt);
        String raw = extractText(chatResponse);
        log.info("suggestTitles: MiniMax 返回助手文本 length={}, 预览(截断至 {} 字符):\n{}",
                raw != null ? raw.length() : 0,
                LOG_RAW_PREVIEW_MAX,
                previewForLog(raw));

        // 6) 期望 raw 形如 ["a","b"]；若解析失败则 parseTitleJson 会按行兜底并打 WARN
        List<String> titles = parseTitleJson(raw);
        log.info("suggestTitles: 解析得到 {} 条标题: {}", titles.size(), titles);
        return new ArticleAiTitlesResponse(titles);
    }

    private MiniMaxChatModel requireModel() {
        MiniMaxChatModel model = chatModelProvider.getIfAvailable();
        if (model == null) {
            throw new IllegalStateException(
                    "未启用 MiniMax：请设置 SPRING_AI_MODEL_CHAT=minimax 与 MINIMAX_API_KEY");
        }
        return model;
    }

    private static String truncate(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= MAX_CONTENT_CHARS) {
            return content;
        }
        return content.substring(0, MAX_CONTENT_CHARS) + "\n\n…（正文已截断用于生成）";
    }

    private static String extractText(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        AssistantMessage output = (AssistantMessage) response.getResult().getOutput();
        if (output.getText() != null && !output.getText().isEmpty()) {
            return output.getText();
        }
        return output.toString();
    }

    private List<String> parseTitleJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String cleaned = stripMarkdownFence(raw.strip());
        try {
            List<String> list = objectMapper.readValue(cleaned, new TypeReference<List<String>>() {});
            if (list == null) {
                return List.of();
            }
            return list.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::strip)
                    .limit(8)
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (Exception e) {
            log.warn("parseTitleJson: 非合法 JSON 数组，将按行兜底解析. err={}", e.getMessage());
            return fallbackLines(cleaned);
        }
    }

    private static String previewForLog(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "(empty)";
        }
        if (raw.length() <= LOG_RAW_PREVIEW_MAX) {
            return raw;
        }
        return raw.substring(0, LOG_RAW_PREVIEW_MAX) + "\n... (" + (raw.length() - LOG_RAW_PREVIEW_MAX) + " 字符未显示)";
    }

    private static String stripMarkdownFence(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) {
                t = t.substring(firstNl + 1);
            }
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
        }
        return t.trim();
    }

    private static List<String> fallbackLines(String cleaned) {
        List<String> out = new ArrayList<>();
        for (String line : cleaned.split("\n")) {
            String x = line.strip()
                    .replaceAll("^[-*\\d.\\s]+", "")
                    .replaceAll("^[\"']|[\"']$", "");
            if (!x.isEmpty()) {
                out.add(x);
            }
            if (out.size() >= 5) {
                break;
            }
        }
        return out;
    }
}
