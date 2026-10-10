package com.blog.api.platform.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AI 模型参数 JSON 解析辅助。
 * <p>{@code ai_model.params} 字段存储 temperature/maxTokens 等 JSON，统一在此解析。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelParams {

    private final ObjectMapper objectMapper;

    /** 解析 params JSON 为 Map；空值返回空 Map */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parse(String paramsJson) {
        if (paramsJson == null || paramsJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(paramsJson, Map.class);
        } catch (Exception e) {
            log.warn("解析模型参数失败，返回空 Map: {}", e.getMessage());
            return Map.of();
        }
    }

    public Double getDouble(Map<String, Object> params, String key, double defaultValue) {
        Object v = params.get(key);
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return defaultValue;
    }

    public Integer getInteger(Map<String, Object> params, String key, int defaultValue) {
        Object v = params.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        return defaultValue;
    }
}
