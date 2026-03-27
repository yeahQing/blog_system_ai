package com.blog.api.service;

import com.blog.api.entity.AdminMenuVisibility;
import com.blog.api.repository.AdminMenuVisibilityRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminMenuVisibilityService {

    public static final List<String> MENU_KEYS = List.of(
            "dashboard",
            "site-settings",
            "articles",
            "categories",
            "tags",
            "friend-links"
    );

    private final AdminMenuVisibilityRepository repository;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onReadyEnsureSingleton() {
        ensureRow();
    }

    private void ensureRow() {
        if (repository.count() == 0) {
            repository.save(AdminMenuVisibility.builder()
                    .id(AdminMenuVisibility.SINGLETON_ID)
                    .build());
        }
    }

    private Map<String, Boolean> defaults() {
        Map<String, Boolean> m = new LinkedHashMap<>();
        for (String k : MENU_KEYS) {
            m.put(k, Boolean.TRUE);
        }
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> getResolved() {
        Map<String, Boolean> out = defaults();
        AdminMenuVisibility row = repository.findById(AdminMenuVisibility.SINGLETON_ID).orElse(null);
        if (row == null || row.getConfigJson() == null || row.getConfigJson().isBlank()) {
            return out;
        }
        try {
            Map<String, Boolean> saved = objectMapper.readValue(row.getConfigJson(), new TypeReference<>() {
            });
            if (saved != null) {
                for (String k : MENU_KEYS) {
                    if (saved.containsKey(k)) {
                        out.put(k, Boolean.TRUE.equals(saved.get(k)));
                    }
                }
            }
        } catch (Exception ignored) {
            /* 使用默认 */
        }
        return out;
    }

    @Transactional
    public Map<String, Boolean> save(Map<String, Boolean> input) {
        ensureRow();
        Map<String, Boolean> merged = defaults();
        if (input != null) {
            for (String k : MENU_KEYS) {
                if (input.containsKey(k)) {
                    merged.put(k, Boolean.TRUE.equals(input.get(k)));
                }
            }
        }
        AdminMenuVisibility row = repository.findById(AdminMenuVisibility.SINGLETON_ID)
                .orElseGet(() -> AdminMenuVisibility.builder().id(AdminMenuVisibility.SINGLETON_ID).build());
        try {
            row.setConfigJson(objectMapper.writeValueAsString(merged));
        } catch (Exception e) {
            throw new IllegalArgumentException("保存菜单配置失败");
        }
        repository.save(row);
        return merged;
    }
}
