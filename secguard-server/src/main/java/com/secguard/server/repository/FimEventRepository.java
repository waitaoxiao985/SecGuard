package com.secguard.server.repository;

import com.secguard.server.entity.FimEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * FIM 变更事件 Repository
 */
@Repository
public interface FimEventRepository extends JpaRepository<FimEventEntity, Long> {

    /** 按 Agent 分页查询 */
    Page<FimEventEntity> findByAgentId(Long agentId, Pageable pageable);

    /** 按 Agent + 事件类型分页查询 */
    Page<FimEventEntity> findByAgentIdAndEventType(Long agentId, String eventType, Pageable pageable);

    /** 按事件类型分页查询 */
    Page<FimEventEntity> findByEventType(String eventType, Pageable pageable);

    /** 统计某 Agent 的 FIM 事件总量 */
    long countByAgentId(Long agentId);
}
