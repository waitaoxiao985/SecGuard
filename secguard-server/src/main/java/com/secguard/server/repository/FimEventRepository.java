package com.secguard.server.repository;

import com.secguard.server.entity.FimEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FIM 变更事件 Repository
 */
@Repository
public interface FimEventRepository extends JpaRepository<FimEventEntity, Long>,
                                             JpaSpecificationExecutor<FimEventEntity> {

    /** 按 Agent 分页查询 */
    Page<FimEventEntity> findByAgentId(Long agentId, Pageable pageable);

    /** 按 Agent + 事件类型分页查询 */
    Page<FimEventEntity> findByAgentIdAndEventType(Long agentId, String eventType, Pageable pageable);

    /** 按事件类型分页查询 */
    Page<FimEventEntity> findByEventType(String eventType, Pageable pageable);

    /** 统计某 Agent 的 FIM 事件总量 */
    long countByAgentId(Long agentId);

    // ===== 统计查询 =====

    /** 按事件类型分组统计 */
    @Query("SELECT e.eventType, COUNT(e) FROM FimEventEntity e GROUP BY e.eventType")
    List<Object[]> countByEventTypeGrouped();

    /** 按 Agent 分组统计 */
    @Query("SELECT e.agentId, COUNT(e) FROM FimEventEntity e GROUP BY e.agentId")
    List<Object[]> countByAgentIdGrouped();

    /** 统计某时间之后的事件数 */
    long countByCreatedAtAfter(LocalDateTime since);

    /** 按天统计事件趋势（原生 SQL，MySQL DATE() 函数） */
    @Query(value = "SELECT DATE(created_at) as event_date, COUNT(*) as cnt " +
                   "FROM sg_fim_event WHERE created_at >= :since " +
                   "GROUP BY DATE(created_at) ORDER BY event_date",
           nativeQuery = true)
    List<Object[]> countByDaySince(@Param("since") LocalDateTime since);

    /** 最活跃的文件路径 TOP N */
    @Query("SELECT e.filePath, COUNT(e) FROM FimEventEntity e " +
           "GROUP BY e.filePath ORDER BY COUNT(e) DESC")
    List<Object[]> findTopFilePaths(Pageable pageable);
}
