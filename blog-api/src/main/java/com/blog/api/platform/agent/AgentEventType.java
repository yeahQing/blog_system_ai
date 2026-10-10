package com.blog.api.platform.agent;

/**
 * SSE 事件类型：智能体对话流的事件分类。
 */
public enum AgentEventType {
    /** 智能体思考/回复文本（流式 chunk） */
    ASSISTANT_MESSAGE,
    /** 工具调用开始 */
    TOOL_CALL,
    /** 工具调用结果 */
    TOOL_RESULT,
    /** 任务完成 */
    DONE,
    /** 错误 */
    ERROR
}
