package com.blog.api.platform.registry;

import com.blog.api.platform.model.AiPrompt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示词注册中心：版本化存取 + 占位符渲染。
 * <p>同一 {@code code} 可有多个版本，仅一个 {@code is_active=true} 生效。
 * 占位符语法 {@code {name}}，渲染时按 {@code variables} Map 替换。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptRegistry {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");

    private final AiPromptRepository promptRepository;

    /**
     * 获取当前生效版本的 Prompt，渲染占位符后返回。
     *
     * @param code      Prompt 业务编码
     * @param variables 占位符值（key 为占位符名，value 为替换值）
     * @return 渲染后的 Prompt 文本
     */
    public String render(String code, Map<String, Object> variables) {
        AiPrompt p = promptRepository.findByCodeAndIsActiveTrue(code)
                .orElseThrow(() -> new IllegalStateException("Prompt 不存在或未激活: " + code));
        return renderPlaceholders(p.getContent(), variables);
    }

    /** 获取当前生效版本的原始 Prompt（不渲染） */
    public Optional<AiPrompt> getActive(String code) {
        return promptRepository.findByCodeAndIsActiveTrue(code);
    }

    /** 查询某 code 的所有版本历史（降序） */
    public List<AiPrompt> getVersions(String code) {
        return promptRepository.findByCodeOrderByVersionDesc(code);
    }

    /**
     * 保存为新版本：同 code 版本号自增，旧版本全部置为非 active，新版本 active。
     */
    @Transactional
    public AiPrompt saveNewVersion(String code, String name, String description, String type,
                                   String content, String variables) {
        promptRepository.clearActiveByCode(code);
        int nextVersion = promptRepository.findMaxVersionByCode(code) + 1;
        AiPrompt p = AiPrompt.builder()
                .code(code)
                .name(name)
                .description(description)
                .type(type)
                .content(content)
                .variables(variables)
                .version(nextVersion)
                .isActive(true)
                .build();
        return promptRepository.save(p);
    }

    /**
     * 回滚到指定版本：把目标版本置 active，其余置非 active。
     */
    @Transactional
    public AiPrompt rollback(Long promptId) {
        AiPrompt target = promptRepository.findById(promptId)
                .orElseThrow(() -> new IllegalArgumentException("Prompt 不存在: id=" + promptId));
        promptRepository.clearActiveByCode(target.getCode());
        target.setIsActive(true);
        return promptRepository.save(target);
    }

    /** 渲染 {placeholder} 占位符 */
    static String renderPlaceholders(String template, Map<String, Object> variables) {
        if (template == null) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            Object v = variables.get(key);
            m.appendReplacement(sb, Matcher.quoteReplacement(v == null ? m.group() : v.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
