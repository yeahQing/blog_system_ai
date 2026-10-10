package com.blog.api.platform.registry;

import com.blog.api.platform.model.AiVectorStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiVectorStoreRepository extends JpaRepository<AiVectorStore, Long> {

    Optional<AiVectorStore> findByCode(String code);

    /** 按业务类型查询启用的 Collection */
    Optional<AiVectorStore> findByBusinessTypeAndEnabledTrue(String businessType);

    List<AiVectorStore> findByEnabledTrue();
}
