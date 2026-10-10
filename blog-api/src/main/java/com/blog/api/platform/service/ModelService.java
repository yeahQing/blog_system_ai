package com.blog.api.platform.service;

import com.blog.api.platform.model.AiModel;
import com.blog.api.platform.registry.AiModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 模型管理服务：CRUD + 切换默认。
 * <p>切换默认时先把同类型其他模型置为非默认（保证唯一）。
 * API Key 加密字段在前端返回时由 Controller 做掩码处理。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelService {

    private final AiModelRepository modelRepository;

    public List<AiModel> listAll() {
        return modelRepository.findAll();
    }

    public List<AiModel> listByType(String type) {
        return modelRepository.findByTypeAndEnabledTrue(type);
    }

    public AiModel getById(Long id) {
        return modelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模型不存在: id=" + id));
    }

    @Transactional
    public AiModel create(AiModel model) {
        if (modelRepository.findByName(model.getName()).isPresent()) {
            throw new IllegalArgumentException("模型名称已存在: " + model.getName());
        }
        // 若新建即为默认，清除同类型旧默认
        if (Boolean.TRUE.equals(model.getIsDefault())) {
            modelRepository.clearDefaultByType(model.getType());
        }
        return modelRepository.save(model);
    }

    @Transactional
    public AiModel update(Long id, AiModel form) {
        AiModel m = getById(id);
        if (form.getName() != null) m.setName(form.getName());
        if (form.getType() != null) m.setType(form.getType());
        if (form.getProvider() != null) m.setProvider(form.getProvider());
        if (form.getModelId() != null) m.setModelId(form.getModelId());
        if (form.getBaseUrl() != null) m.setBaseUrl(form.getBaseUrl());
        // apiKeyEncrypted 仅在非空时更新（前端传掩码则忽略）
        if (form.getApiKeyEncrypted() != null && !form.getApiKeyEncrypted().isBlank()) {
            m.setApiKeyEncrypted(form.getApiKeyEncrypted());
        }
        if (form.getDimension() != null) m.setDimension(form.getDimension());
        if (form.getParams() != null) m.setParams(form.getParams());
        if (form.getEnabled() != null) m.setEnabled(form.getEnabled());
        return modelRepository.save(m);
    }

    /** 设为默认（同类型其他置非默认） */
    @Transactional
    public AiModel setDefault(Long id) {
        AiModel m = getById(id);
        modelRepository.clearDefaultByType(m.getType());
        m.setIsDefault(true);
        return modelRepository.save(m);
    }

    @Transactional
    public void delete(Long id) {
        AiModel m = getById(id);
        if (Boolean.TRUE.equals(m.getIsDefault())) {
            throw new IllegalStateException("默认模型不可删除，请先切换默认");
        }
        modelRepository.deleteById(id);
    }
}
