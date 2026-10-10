package com.blog.api.platform.registry;

import com.blog.api.platform.model.AiMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiMemoryRepository extends JpaRepository<AiMemory, Long> {

    Optional<AiMemory> findBySessionId(String sessionId);

    List<AiMemory> findByUserIdOrderByLastActiveAtDesc(Long userId);
}
