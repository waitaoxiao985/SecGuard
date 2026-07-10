package com.secguard.server.repository;

import com.secguard.server.entity.LogEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 日志事件 Repository
 */
@Repository
public interface LogEventRepository extends JpaRepository<LogEventEntity, Long> {

    /** 按 Agent 分页查询 */
    Page<LogEventEntity> findByAgentId(Long agentId, Pageable pageable);

    /** 按类别分页查询 */
    Page<LogEventEntity> findByCategory(String category, Pageable pageable);

    /** 按 Agent + 类别分页查询 */
    Page<LogEventEntity> findByAgentIdAndCategory(Long agentId, String category, Pageable pageable);

    /** 统计某 Agent 的日志总量 */
    long countByAgentId(Long agentId);

    /** 按时间范围统计 */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /** 清理指定时间之前的日志 */
    void deleteByCreatedAtBefore(LocalDateTime threshold);
}
