package com.blog.api.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.blog.api.platform.model.AiMemory;
import com.blog.api.platform.registry.AiMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 记忆管理服务：会话上下文存储 + 滑动窗口压缩。
 * <p>单会话最多保留最近 {@link #MAX_TURNS} 轮（一轮 = USER + ASSISTANT），
 * 超出时旧消息压缩进 summary。messages 存精简版（role + content），不含工具细节。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {

    /** 单会话保留的最大轮数（USER+ASSISTANT 各算一条，即最多 2*MAX_TURNS 条消息） */
    private static final int MAX_TURNS = 20;

    private final AiMemoryRepository memoryRepository;
    private final ObjectMapper objectMapper;

    /** 加载会话历史消息（精简版，供 ChatClient 的 messages 参数用） */
    public List<Map<String, String>> loadHistory(String sessionId) {
        Optional<AiMemory> opt = memoryRepository.findBySessionId(sessionId);
        if (opt.isEmpty()) {
            return List.of();
        }
        return parseMessages(opt.get().getMessages());
    }

    /** 获取会话摘要（长对话压缩） */
    public Optional<String> getSummary(String sessionId) {
        return memoryRepository.findBySessionId(sessionId).map(AiMemory::getSummary);
    }

    /**
     * 保存一轮对话（USER + ASSISTANT），应用滑动窗口。
     */
    @Transactional
    public void appendTurn(String sessionId, Long userId, String userMessage, String assistantMessage) {
        AiMemory memory = memoryRepository.findBySessionId(sessionId).orElseGet(() ->
                AiMemory.builder()
                        .sessionId(sessionId)
                        .userId(userId)
                        .messages("[]")
                        .build());

        List<Map<String, String>> msgs = parseMessages(memory.getMessages());
        // 追加本轮
        if (userMessage != null) {
            msgs.add(messageMap("user", userMessage));
        }
        if (assistantMessage != null) {
            msgs.add(messageMap("assistant", assistantMessage));
        }

        // 滑动窗口：超出 MAX_TURNS 轮（即 2*MAX_TURNS 条）时，丢弃最旧一轮
        int maxItems = MAX_TURNS * 2;
        while (msgs.size() > maxItems) {
            // 丢弃最旧的 user + assistant（若首条是 assistant 则单独丢）
            msgs.remove(0);
            if (!msgs.isEmpty() && "assistant".equals(msgs.get(0).get("role"))) {
                // 压缩进 summary（简化处理：累加被丢弃内容的提示）
                appendSummary(memory, "（更早的对话已省略）");
                msgs.remove(0);
            }
        }

        memory.setMessages(toJson(msgs));
        memory.setLastActiveAt(LocalDateTime.now());
        memoryRepository.save(memory);
    }

    /** 列出用户的所有会话（最近活跃优先） */
    public List<AiMemory> listByUser(Long userId) {
        return memoryRepository.findByUserIdOrderByLastActiveAtDesc(userId);
    }

    /** 查询会话记忆详情 */
    public Optional<AiMemory> getBySession(String sessionId) {
        return memoryRepository.findBySessionId(sessionId);
    }

    /** 清除会话记忆 */
    @Transactional
    public void clear(String sessionId) {
        memoryRepository.findBySessionId(sessionId).ifPresent(memoryRepository::delete);
    }

    // ---------- 内部工具 ----------

    private List<Map<String, String>> parseMessages(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("解析记忆消息失败，返回空列表: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String toJson(List<Map<String, String>> msgs) {
        try {
            return objectMapper.writeValueAsString(msgs);
        } catch (Exception e) {
            log.warn("序列化记忆消息失败: {}", e.getMessage());
            return "[]";
        }
    }

    private Map<String, String> messageMap(String role, String content) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private void appendSummary(AiMemory memory, String text) {
        String existing = memory.getSummary();
        memory.setSummary(existing == null ? text : existing + "\n" + text);
    }
}
