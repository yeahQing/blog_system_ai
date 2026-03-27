package com.blog.api.repository;

import com.blog.api.entity.FriendLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendLinkRepository extends JpaRepository<FriendLink, Long> {

    List<FriendLink> findByEnabledTrueOrderBySortOrderAsc();

    long countByEnabledTrue();
}
