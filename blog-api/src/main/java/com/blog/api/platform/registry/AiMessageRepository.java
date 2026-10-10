package com.blog.api.platform.registry;

import com.blog.api.platform.model.AiMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    /** 按会话查询消息流（按时间升序） */
    List<AiMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /** 按执行 ID 查询单次执行轨迹 */
    List<AiMessage> findByAgentRunIdOrderByCreatedAtAsc(String agentRunId);

    /** 按会话分页查询 */
    Page<AiMessage> findBySessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);

    /** 按角色过滤查询 */
    List<AiMessage> findBySessionIdAndRoleOrderByCreatedAtAsc(String sessionId, String role);
}
