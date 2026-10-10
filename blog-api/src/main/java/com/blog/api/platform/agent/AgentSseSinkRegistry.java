package com.blog.api.platform.agent;

import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SSE Sink 注册中心：按 sessionId 存储当前智能体执行的 sink。
 * <p>工具类是单例，无法用 ThreadLocal 可靠传递 sink（Spring AI 工具调用可能在线程池线程执行）。
 * 本类用 sessionId 隔离，AgentExecutor 执行前注册 sink，工具执行时按当前 sessionId 取 sink 推送事件。</p>
 * <p>sink 类型用 {@link Consumer} 抽象（兼容 FluxSink 和任意回调），controller 可传入
 * SseEmitter.send 的 lambda 包装。</p>
 */
public class AgentSseSinkRegistry {

    private static final ConcurrentMap<String, Consumer<AgentMessage>> SINKS = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> CURRENT_SESSION = new ThreadLocal<>();

    /** AgentExecutor 注册 sink（sink 可为 null，表示不推送只写 ai_message） */
    public static void register(String sessionId, Consumer<AgentMessage> sink) {
        if (sink == null) return;
        SINKS.put(sessionId, sink);
    }

    /** AgentExecutor 注销 sink */
    public static void unregister(String sessionId) {
        SINKS.remove(sessionId);
    }

    /** 设置当前线程的 sessionId（工具调用前） */
    public static void setCurrentSession(String sessionId) {
        CURRENT_SESSION.set(sessionId);
    }

    public static void clearCurrentSession() {
        CURRENT_SESSION.remove();
    }

    /** 工具调用通知：推送 TOOL_CALL 事件 */
    public static void notifyToolCall(String toolName, String args) {
        push(AgentMessage.toolCall(toolName, args));
    }

    /** 工具调用通知：推送 TOOL_RESULT 事件 */
    public static void notifyToolResult(String toolName, String resultSummary) {
        push(AgentMessage.toolResult(toolName, resultSummary));
    }

    private static void push(AgentMessage msg) {
        String sessionId = CURRENT_SESSION.get();
        if (sessionId == null) {
            return;
        }
        Consumer<AgentMessage> sink = SINKS.get(sessionId);
        if (sink != null) {
            sink.accept(msg);
        }
    }

    private AgentSseSinkRegistry() {}
}
