package com.blog.api.platform.registry;

import com.blog.api.platform.model.AiModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiModelRepository extends JpaRepository<AiModel, Long> {

    Optional<AiModel> findByName(String name);

    /** 按类型查询启用的模型 */
    List<AiModel> findByTypeAndEnabledTrue(String type);

    /** 查询某类型的默认模型 */
    Optional<AiModel> findByTypeAndIsDefaultTrue(String type);

    /** 切换默认：先把同类型其他模型置为非默认 */
    @Modifying
    @Query("UPDATE AiModel m SET m.isDefault = false WHERE m.type = :type")
    void clearDefaultByType(@Param("type") String type);
}
