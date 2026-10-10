package com.blog.api.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.blog.api.platform.model.AiMessage;
import com.blog.api.platform.registry.AiMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息存储服务：持久化智能体对话的完整消息流（USER/ASSISTANT/TOOL）。
 * <p>这是可观测性的核心——每次工具调用、模型响应都落库，支持按会话/执行ID查询轨迹。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageStoreService {

    private final AiMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    /** 记录用户消息 */
    public AiMessage saveUserMessage(String sessionId, String agentRunId, String content) {
        return save(AiMessage.builder()
                .sessionId(sessionId)
                .agentRunId(agentRunId)
                .role("USER")
                .content(content)
                .build());
    }

    /** 记录 System 消息 */
    public AiMessage saveSystemMessage(String sessionId, String agentRunId, String content) {
        return save(AiMessage.builder()
                .sessionId(sessionId)
                .agentRunId(agentRunId)
                .role("SYSTEM")
                .content(content)
                .build());
    }

    /** 记录助手消息（含模型名、token、耗时） */
    public AiMessage saveAssistantMessage(String sessionId, String agentRunId, String content,
                                          String modelName, Integer tokenInput, Integer tokenOutput, Integer latencyMs) {
        return save(AiMessage.builder()
                .sessionId(sessionId)
                .agentRunId(agentRunId)
                .role("ASSISTANT")
                .content(content)
                .modelName(modelName)
                .tokenInput(tokenInput)
                .tokenOutput(tokenOutput)
                .latencyMs(latencyMs)
                .build());
    }

    /** 记录工具调用（入参） */
    public AiMessage saveToolCall(String sessionId, String agentRunId, String toolName,
                                  String toolCallId, Object toolArgs) {
        return save(AiMessage.builder()
                .sessionId(sessionId)
                .agentRunId(agentRunId)
                .role("TOOL")
                .toolName(toolName)
                .toolCallId(toolCallId)
                .toolArgs(toJson(toolArgs))
                .build());
    }

    /** 记录工具返回结果（更新对应 TOOL 消息或新增一条） */
    public AiMessage saveToolResult(String sessionId, String agentRunId, String toolName,
                                    String toolCallId, Object toolResult) {
        return save(AiMessage.builder()
                .sessionId(sessionId)
                .agentRunId(agentRunId)
                .role("TOOL")
                .toolName(toolName)
                .toolCallId(toolCallId)
                .toolResult(toJson(toolResult))
                .build());
    }

    /** 按会话查询完整消息流（时间升序） */
    public List<AiMessage> getBySession(String sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /** 按执行 ID 查询单次执行轨迹 */
    public List<AiMessage> getByRunId(String agentRunId) {
        return messageRepository.findByAgentRunIdOrderByCreatedAtAsc(agentRunId);
    }

    /** 按会话分页查询（最新优先） */
    public Page<AiMessage> getBySessionPaged(String sessionId, Pageable pageable) {
        return messageRepository.findBySessionIdOrderByCreatedAtDesc(sessionId, pageable);
    }

    /** 按会话+角色过滤查询 */
    public List<AiMessage> getBySessionAndRole(String sessionId, String role) {
        return messageRepository.findBySessionIdAndRoleOrderByCreatedAtAsc(sessionId, role);
    }

    private AiMessage save(AiMessage m) {
        return messageRepository.save(m);
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("序列化工具参数失败: {}", e.getMessage());
            return String.valueOf(obj);
        }
    }
}
