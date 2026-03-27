package com.blog.api.repository;

import com.blog.api.entity.GuestbookMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuestbookRepository extends JpaRepository<GuestbookMessage, Long> {

    Page<GuestbookMessage> findByApprovedTrueOrderByCreatedAtDesc(Pageable pageable);
}
