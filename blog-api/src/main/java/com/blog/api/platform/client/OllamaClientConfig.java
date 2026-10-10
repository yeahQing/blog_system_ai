package com.blog.api.platform.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Ollama HTTP 客户端配置：提供专用 RestTemplate Bean。
 * <p>Ollama 模型配置（base-url / model）从 {@link com.blog.api.platform.model.AiModel} 动态读取。</p>
 */
@Configuration
public class OllamaClientConfig {

    @Bean(name = "ollamaRestTemplate")
    public RestTemplate ollamaRestTemplate(
            RestTemplateBuilder builder,
            @Value("${ollama.timeout-ms:30000}") int timeoutMs) {
        return builder
                .setConnectTimeout(Duration.ofMillis(Math.min(timeoutMs, 15000)))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }
}
