package com.blog.api.platform.rag;

import com.blog.api.platform.registry.ModelRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.minimax.MiniMaxChatOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 重排序服务：用 MiniMax 对检索候选块按查询相关性重新打分排序。
 * <p>语义检索召回的 Top-N 可能含噪声（向量相近但语义不相关），
 * 用 LLM 对每个候选与 query 的相关性打分（0-10），重新排序提升精度。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RerankService {

    private final ModelRegistry modelRegistry;
    private final ObjectMapper objectMapper;

    /**
     * 对候选块重排序。
     *
     * @param query      查询文本
     * @param candidates 候选块（已检索）
     * @param topN       返回条数
     * @return 按相关性降序的 topN 块
     */
    public List<ChunkSearchHit> rerank(String query, List<ChunkSearchHit> candidates, int topN) {
        if (candidates == null || candidates.size() <= topN) {
            return candidates == null ? List.of() : candidates;
        }
        try {
            Map<Long, Double> scores = scoreWithLLM(query, candidates);
            // 按 LLM 分数降序，分数相同则保留原语义分数顺序
            List<ChunkSearchHit> reranked = new ArrayList<>(candidates);
            reranked.sort(Comparator.<ChunkSearchHit>comparingDouble(h -> scores.getOrDefault(h.getChunkId(), 0.0)).reversed()
                    .thenComparing(Comparator.comparingDouble(ChunkSearchHit::getScore).reversed()));
            // 更新 score 为重排序后的归一化分数（0-1）
            reranked.forEach(h -> {
                double s = scores.getOrDefault(h.getChunkId(), h.getScore());
                h.setScore(s / 10.0);
            });
            return reranked.stream().limit(topN).toList();
        } catch (Exception e) {
            log.warn("重排序失败，回退到原排序: {}", e.getMessage());
            return candidates.stream()
                    .sorted(Comparator.comparingDouble(ChunkSearchHit::getScore).reversed())
                    .limit(topN)
                    .toList();
        }
    }

    /**
     * 调用 MiniMax 对每个候选打分。
     * 让模型返回 JSON：[{"id":1,"score":9.0},...]
     */
    @SuppressWarnings("unchecked")
    private Map<Long, Double> scoreWithLLM(String query, List<ChunkSearchHit> candidates) throws Exception {
        ChatModel chatModel = modelRegistry.getDefaultChatModel();
        StringBuilder candText = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            ChunkSearchHit h = candidates.get(i);
            candText.append("ID=").append(h.getChunkId()).append("：")
                    .append(h.getContent().replaceAll("\\s+", " ")
                            .substring(0, Math.min(200, h.getContent().length())))
                    .append("\n");
        }
        String systemPrompt = """
                你是相关性评估专家。给定查询和多个文本块，对每个文本块与查询的相关性打分（0-10，10 最相关）。
                只输出 JSON 数组，元素为 {"id":块ID,"score":分数}，不要任何其他文字。
                """;
        String userPrompt = "查询：" + query + "\n\n候选文本块：\n" + candText;
        Prompt prompt = new Prompt(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                MiniMaxChatOptions.builder().maxTokens(1024).temperature(0.1).build());
        String reply = chatModel.call(prompt).getResult().getOutput().getText();

        // 解析 JSON
        String cleaned = stripMarkdownFence(reply.trim());
        List<Map<String, Object>> scored = objectMapper.readValue(cleaned, new TypeReference<>() {});
        return scored.stream().collect(Collectors.toMap(
                m -> ((Number) m.get("id")).longValue(),
                m -> ((Number) m.get("score")).doubleValue(),
                (a, b) -> a));
    }

    private static String stripMarkdownFence(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            if (nl > 0) t = t.substring(nl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }
}
