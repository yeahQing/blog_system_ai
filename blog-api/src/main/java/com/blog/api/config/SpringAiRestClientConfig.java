package com.blog.api.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Spring AI RestClient 超时配置。
 * <p>Spring AI 的 MiniMaxChatAutoConfiguration 会注入 {@code RestClient.Builder}，
 * 默认超时较短（约 10s），但智能体带工具调用时单次请求可能耗时 30-60s+。
 * 本配置提供一个长超时的 Builder，覆盖默认值。</p>
 */
@Configuration
public class SpringAiRestClientConfig {

    /** 智能体对话 + 工具调用，单次请求最长等待 120s */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(120);

    /**
     * 提供带长超时的 RestClient.Builder，Spring AI 会自动用它构建 MiniMaxApi。
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withReadTimeout(READ_TIMEOUT);
        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(settings);
        return RestClient.builder().requestFactory(factory);
    }
}
