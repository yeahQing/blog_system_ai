package com.blog.api.platform.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Ollama Embedding 调用封装：单条 / 批量向量化。
 * <p>直接调用 Ollama /api/embed 接口，不依赖 Spring AI Ollama 模块。</p>
 */
@Slf4j
@Component
public class OllamaEmbeddingClient {

    private final RestTemplate ollamaRestTemplate;
    private final ObjectMapper objectMapper;

    public OllamaEmbeddingClient(@Qualifier("ollamaRestTemplate") RestTemplate ollamaRestTemplate,
                                 ObjectMapper objectMapper) {
        this.ollamaRestTemplate = ollamaRestTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 调用 Ollama /api/embed 接口向量化单条文本。
     *
     * @param baseUrl Ollama 地址，如 http://blog-ollama:11434
     * @param model   模型名，如 bge-m3
     * @param text    待向量化的文本
     * @return 浮点向量
     */
    @SuppressWarnings("unchecked")
    public float[] embed(String baseUrl, String model, String text) {
        String url = stripTrailingSlash(baseUrl) + "/api/embed";
        Map<String, Object> body = Map.of("model", model, "input", text);
        Map<String, Object> resp = ollamaRestTemplate.postForObject(url, body, Map.class);
        if (resp == null) {
            throw new IllegalStateException("Ollama embedding 返回空响应");
        }
        List<List<Number>> embeddings = (List<List<Number>>) resp.get("embeddings");
        if (embeddings == null || embeddings.isEmpty()) {
            throw new IllegalStateException("Ollama embedding 返回空向量");
        }
        List<Number> vec = embeddings.get(0);
        float[] result = new float[vec.size()];
        for (int i = 0; i < vec.size(); i++) {
            result[i] = vec.get(i).floatValue();
        }
        return result;
    }

    /** 批量向量化（逐条调用，Ollama /api/embed 的 input 支持数组，但单条更稳） */
    public List<float[]> embedBatch(String baseUrl, String model, List<String> texts) {
        return texts.stream()
                .map(t -> embed(baseUrl, model, t))
                .toList();
    }

    private static String stripTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
