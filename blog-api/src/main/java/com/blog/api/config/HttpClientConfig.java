package com.blog.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * 通用 {@link RestTemplate} 与封面专用实例分离：文生图单独使用较短读写超时，超时后抛出异常并返回，避免长时间阻塞。
 */
@Configuration
public class HttpClientConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate(
            RestTemplateBuilder builder,
            @Value("${blog.http-client.connect-timeout-ms:15000}") int connectTimeoutMs,
            @Value("${blog.http-client.read-timeout-ms:180000}") int readTimeoutMs) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

    /**
     * MiniMax 文生图 + 下载 + 图床：独立超时，默认读超时 90s（可用 blog.ai-cover.http-read-timeout-ms 调整）。
     */
    @Bean(name = "articleCoverRestTemplate")
    public RestTemplate articleCoverRestTemplate(
            RestTemplateBuilder builder,
            @Value("${blog.ai-cover.http-connect-timeout-ms:10000}") int connectTimeoutMs,
            @Value("${blog.ai-cover.http-read-timeout-ms:90000}") int readTimeoutMs) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

    /** MiniMax 文生图 POST：与示例一致使用 OkHttp，超时与 blog.ai-cover.http-*-timeout-ms 对齐 */
    @Bean
    public OkHttpClient minimaxImageOkHttpClient(
            @Value("${blog.ai-cover.http-connect-timeout-ms:10000}") long connectTimeoutMs,
            @Value("${blog.ai-cover.http-read-timeout-ms:90000}") long readTimeoutMs) {
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(60_000, TimeUnit.MILLISECONDS)
                .build();
    }
}
