package com.blog.api.platform.service;

import com.blog.api.platform.model.AiPrompt;
import com.blog.api.platform.registry.PromptRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 提示词管理服务：CRUD 委托 {@link PromptRegistry}，提供版本历史/回滚。
 */
@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptRegistry promptRegistry;

    /** 获取某 code 的当前生效版本 */
    public Optional<AiPrompt> getActive(String code) {
        return promptRegistry.getActive(code);
    }

    /** 查询某 code 的所有版本 */
    public List<AiPrompt> getVersions(String code) {
        return promptRegistry.getVersions(code);
    }

    /** 保存为新版本 */
    public AiPrompt saveNewVersion(String code, String name, String description, String type,
                                   String content, String variables) {
        return promptRegistry.saveNewVersion(code, name, description, type, content, variables);
    }

    /** 回滚到指定版本 */
    public AiPrompt rollback(Long promptId) {
        return promptRegistry.rollback(promptId);
    }
}
