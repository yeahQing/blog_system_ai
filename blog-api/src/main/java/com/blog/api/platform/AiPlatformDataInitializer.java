package com.blog.api.platform;

import com.blog.api.platform.model.AiModel;
import com.blog.api.platform.model.AiPrompt;
import com.blog.api.platform.registry.AiModelRepository;
import com.blog.api.platform.registry.AiPromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 平台数据初始化：启动时幂等插入默认模型配置 + Prompt 模板。
 * <p>仅当 ai_model / ai_prompt 表为空时插入，已存在则跳过（不会覆盖用户的修改）。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiPlatformDataInitializer {

    private final AiModelRepository modelRepository;
    private final AiPromptRepository promptRepository;

    @Bean
    public ApplicationRunner initAiPlatformData() {
        return args -> {
            initModels();
            initPrompts();
        };
    }

    @Transactional
    protected void initModels() {
        if (modelRepository.count() > 0) {
            log.info("ai_model 已有数据，跳过初始化");
            return;
        }
        // 默认 CHAT 模型：MiniMax（API Key 从环境变量读取，这里只存占位）
        String minimaxKey = System.getenv().getOrDefault("MINIMAX_API_KEY", "");
        AiModel chat = AiModel.builder()
                .name("minimax-m2.7")
                .type("CHAT")
                .provider("MINIMAX")
                .modelId("MiniMax-M2.7")
                .baseUrl("https://api.minimax.chat")
                .apiKeyEncrypted(minimaxKey)
                .params("{\"temperature\":0.4,\"maxTokens\":4096}")
                .isDefault(true)
                .enabled(true)
                .build();
        // 默认 EMBEDDING 模型：Ollama bge-m3（地址可由环境变量覆盖）
        String ollamaUrl = System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
        AiModel embedding = AiModel.builder()
                .name("ollama-bge-m3")
                .type("EMBEDDING")
                .provider("OLLAMA")
                .modelId("bge-m3")
                .baseUrl(ollamaUrl)
                .dimension(1024)
                .params(null)
                .isDefault(true)
                .enabled(true)
                .build();
        modelRepository.saveAll(java.util.List.of(chat, embedding));
        log.info("已初始化默认模型：minimax-m2.7 (CHAT), ollama-bge-m3 (EMBEDDING)");
    }

    @Transactional
    protected void initPrompts() {
        if (promptRepository.count() > 0) {
            log.info("ai_prompt 已有数据，跳过初始化");
            return;
        }
        // 智能体系统提示词
        savePrompt("agent_system", "智能体系统提示词",
                "定义智能体角色、可用工具、写作流程",
                "SYSTEM",
                """
                你是「AI BLOG」博客系统的创作助手智能体。你可以帮博主写文章、查文章、改文章。

                你的能力（工具）：
                1. search_articles - 语义搜索历史文章
                2. get_article_detail - 读取文章详情
                3. generate_outline - 生成文章大纲
                4. write_section - 写作单个章节（基于 RAG 检索历史内容）
                5. generate_summary - 生成摘要
                6. suggest_titles - 推荐标题
                7. generate_cover - 生成封面图
                8. save_draft - 保存草稿

                写文章的标准流程：
                1. 先用 search_articles 检查是否已有类似文章
                2. 用 generate_outline 生成大纲
                3. 逐个章节调用 write_section 写作
                4. 用 generate_summary 生成摘要
                5. 用 suggest_titles 推荐 3 个标题供选择
                6. 用 save_draft 保存草稿

                注意：
                - 写技术博客要准确，不要编造不存在的 API 或命令
                - 使用简体中文，Markdown 格式
                - 每完成一步，简要告诉用户进度
                """,
                "[]");

        // 章节写作提示词（RAG 增强版）
        savePrompt("write_section", "章节写作（RAG）",
                "基于检索到的历史内容写作单个章节",
                "TEMPLATE",
                """
                你正在写一篇技术博客。以下是博客中已有的相关内容，请参考其风格和事实，但不要直接复制：

                【检索到的相关内容】
                {retrieved_context}

                【当前文章主题】{article_topic}
                【本章节标题】{section_title}
                【已写章节】{previous_sections}

                请写本章节正文，要求：
                - 简体中文，Markdown 格式
                - 风格与检索到的内容保持一致
                - 如果检索内容中有可复用的事实/命令，优先使用，不要编造替代品
                - 与已写章节保持连贯，不重复
                """,
                "[{\"name\":\"retrieved_context\",\"desc\":\"检索到的相关内容\"},{\"name\":\"article_topic\",\"desc\":\"文章主题\"},{\"name\":\"section_title\",\"desc\":\"章节标题\"},{\"name\":\"previous_sections\",\"desc\":\"已写章节\"}]");

        // 摘要生成提示词
        savePrompt("generate_summary", "生成摘要",
                "根据正文生成文章摘要",
                "TEMPLATE",
                """
                你是中文技术博客编辑。请根据用户给出的 Markdown 或纯文本正文，写一段用于文章列表页展示的摘要。
                要求：使用简体中文；不超过 220 字；客观概括主题与要点；不编造正文中没有的信息；不要加引号或「摘要：」等前缀。

                正文：
                {content}
                """,
                "[{\"name\":\"content\",\"desc\":\"文章正文\"}]");

        log.info("已初始化默认 Prompt 模板（agent_system / write_section / generate_summary）");
    }

    private void savePrompt(String code, String name, String description, String type,
                            String content, String variables) {
        AiPrompt p = AiPrompt.builder()
                .code(code)
                .name(name)
                .description(description)
                .type(type)
                .content(content)
                .variables(variables)
                .version(1)
                .isActive(true)
                .build();
        promptRepository.save(p);
    }
}
