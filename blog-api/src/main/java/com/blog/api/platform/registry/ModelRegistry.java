package com.blog.api.platform.registry;

import com.blog.api.platform.client.OllamaEmbeddingClient;
import com.blog.api.platform.model.AiModel;
import com.blog.api.platform.model.AiModelParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 模型注册中心：从 {@code ai_model} 表读取配置，运行时动态提供 ChatModel / Embedding 调用。
 * <p>设计取舍：
 * <ul>
 *   <li>CHAT 模型：MiniMax 已由 Spring AI 自动配置注入，本类复用该 Bean（后续接多 provider 时再改造为动态构建）</li>
 *   <li>EMBEDDING 模型：当前仅支持 Ollama，通过 {@link OllamaEmbeddingClient} 动态调用</li>
 * </ul>
 * 换模型只需在后台改数据库 {@code ai_model.is_default}，无需重启应用。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRegistry {

    private final AiModelRepository modelRepository;
    private final AiModelParams aiModelParams;
    private final OllamaEmbeddingClient ollamaEmbeddingClient;
    /** Spring AI 自动配置的 MiniMaxChatModel，未配置时为 null */
    private final ObjectProvider<ChatModel> chatModelProvider;

    /**
     * 获取默认 CHAT 模型对应的 ChatModel Bean。
     * <p>当前实现：默认 CHAT 模型必须是 MiniMax（与 Spring AI 自动配置的 Bean 一致）；
     * 若后台配置了非 MiniMax 的默认 CHAT 模型，将抛出不支持异常（v1.2 暂未实现动态构建）。</p>
     */
    public ChatModel getDefaultChatModel() {
        AiModel m = modelRepository.findByTypeAndIsDefaultTrue("CHAT")
                .orElseThrow(() -> new IllegalStateException("未配置默认 CHAT 模型，请在「AI 控制台-模型管理」中设置"));
        if (!"MINIMAX".equalsIgnoreCase(m.getProvider())) {
            throw new IllegalStateException("暂不支持的 CHAT provider: " + m.getProvider() + "（v1.2 仅支持 MINIMAX）");
        }
        ChatModel bean = chatModelProvider.getIfAvailable();
        if (bean == null) {
            throw new IllegalStateException(
                    "MiniMax ChatModel 未启用：请设置 SPRING_AI_MODEL_CHAT=minimax 与 MINIMAX_API_KEY");
        }
        return bean;
    }

    /**
     * 对默认 EMBEDDING 模型向量化单条文本。
     * <p>当前实现：默认 EMBEDDING 模型必须是 Ollama。</p>
     */
    public float[] embedWithDefault(String text) {
        AiModel m = getDefaultEmbeddingModel();
        if (!"OLLAMA".equalsIgnoreCase(m.getProvider())) {
            throw new IllegalStateException("暂不支持的 EMBEDDING provider: " + m.getProvider() + "（v1.2 仅支持 OLLAMA）");
        }
        return ollamaEmbeddingClient.embed(m.getBaseUrl(), m.getModelId(), text);
    }

    /** 批量向量化 */
    public List<float[]> embedBatchWithDefault(List<String> texts) {
        AiModel m = getDefaultEmbeddingModel();
        if (!"OLLAMA".equalsIgnoreCase(m.getProvider())) {
            throw new IllegalStateException("暂不支持的 EMBEDDING provider: " + m.getProvider());
        }
        return ollamaEmbeddingClient.embedBatch(m.getBaseUrl(), m.getModelId(), texts);
    }

    /** 获取默认 EMBEDDING 模型配置（含维度） */
    public AiModel getDefaultEmbeddingModel() {
        return modelRepository.findByTypeAndIsDefaultTrue("EMBEDDING")
                .orElseThrow(() -> new IllegalStateException("未配置默认 EMBEDDING 模型，请在「AI 控制台-模型管理」中设置"));
    }

    /** 按名称获取模型配置 */
    public Optional<AiModel> getByName(String name) {
        return modelRepository.findByName(name);
    }

    /** 默认 EMBEDDING 维度（RAG 建表时需要） */
    public int getDefaultEmbeddingDimension() {
        Integer dim = getDefaultEmbeddingModel().getDimension();
        if (dim == null || dim <= 0) {
            throw new IllegalStateException("默认 EMBEDDING 模型未配置 dimension");
        }
        return dim;
    }
}
