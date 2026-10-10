package com.blog.api;

import org.springframework.ai.model.minimax.autoconfigure.MiniMaxEmbeddingAutoConfiguration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.blog.api.entity.User;
import com.blog.api.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 博客系统后端入口
 */
@Slf4j
@SpringBootApplication(exclude = { MiniMaxEmbeddingAutoConfiguration.class })
@EnableAsync
public class BlogApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogApiApplication.class, args);
        log.info("Blog API Application started successfully!");
    }

    /**
     * 初始化管理员账号
     */
    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .nickname("管理员")
                        .email("admin@blog.com")
                        .build();
                userRepository.save(admin);
                log.info("初始化管理员账号成功: admin / admin123");
            }
        };
    }

}
