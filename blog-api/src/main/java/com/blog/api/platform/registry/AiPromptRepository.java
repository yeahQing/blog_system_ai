package com.blog.api.platform.registry;

import com.blog.api.platform.model.AiPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiPromptRepository extends JpaRepository<AiPrompt, Long> {

    /** 查询某 code 的当前生效版本 */
    Optional<AiPrompt> findByCodeAndIsActiveTrue(String code);

    /** 查询某 code 的所有版本（按版本号降序） */
    List<AiPrompt> findByCodeOrderByVersionDesc(String code);

    /** 切换生效：先把同 code 其他版本置为非 active */
    @Modifying
    @Query("UPDATE AiPrompt p SET p.isActive = false WHERE p.code = :code")
    void clearActiveByCode(@Param("code") String code);

    /** 查询某 code 的最大版本号 */
    @Query("SELECT COALESCE(MAX(p.version), 0) FROM AiPrompt p WHERE p.code = :code")
    Integer findMaxVersionByCode(@Param("code") String code);
}
