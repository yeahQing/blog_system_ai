package com.blog.api.service;

import com.blog.api.entity.FriendLink;
import com.blog.api.repository.FriendLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendLinkService {

    private final FriendLinkRepository friendLinkRepository;

    public List<FriendLink> listEnabled() {
        return friendLinkRepository.findByEnabledTrueOrderBySortOrderAsc();
    }

    public List<FriendLink> listAll() {
        return friendLinkRepository.findAll();
    }

    @Transactional
    public FriendLink save(FriendLink link) {
        if (link.getName() == null || link.getName().isBlank()) {
            throw new IllegalArgumentException("名称不能为空");
        }
        if (link.getUrl() == null || link.getUrl().isBlank()) {
            throw new IllegalArgumentException("链接不能为空");
        }
        if (link.getSortOrder() == null) {
            link.setSortOrder(0);
        }
        if (link.getEnabled() == null) {
            link.setEnabled(true);
        }
        return friendLinkRepository.save(link);
    }

    @Transactional
    public void delete(Long id) {
        friendLinkRepository.deleteById(id);
    }
}
