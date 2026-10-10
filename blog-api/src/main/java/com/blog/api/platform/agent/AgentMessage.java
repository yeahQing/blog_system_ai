package com.blog.api.platform.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能体对话消息（SSE 推送单元）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessage {
    /** 事件类型 */
    private AgentEventType type;
    /** 文本内容（ASSISTANT_MESSAGE 的 chunk，或工具描述） */
    private String content;
    /** 工具名（TOOL_CALL / TOOL_RESULT） */
    private String toolName;
    /** 工具入参/结果（JSON 字符串） */
    private String toolData;

    public static AgentMessage assistant(String chunk) {
        return AgentMessage.builder().type(AgentEventType.ASSISTANT_MESSAGE).content(chunk).build();
    }

    public static AgentMessage toolCall(String toolName, String args) {
        return AgentMessage.builder().type(AgentEventType.TOOL_CALL).toolName(toolName).toolData(args).build();
    }

    public static AgentMessage toolResult(String toolName, String result) {
        return AgentMessage.builder().type(AgentEventType.TOOL_RESULT).toolName(toolName).toolData(result).build();
    }

    public static AgentMessage done(String summary) {
        return AgentMessage.builder().type(AgentEventType.DONE).content(summary).build();
    }

    public static AgentMessage error(String msg) {
        return AgentMessage.builder().type(AgentEventType.ERROR).content(msg).build();
    }
}
