package com.secguard.server.repository;

import com.secguard.server.entity.FimBaselineEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * FIM 基线 Repository
 */
@Repository
public interface FimBaselineRepository extends JpaRepository<FimBaselineEntity, Long> {

    /** 按 Agent 分页查询基线 */
    Page<FimBaselineEntity> findByAgentId(Long agentId, Pageable pageable);

    /** 按 Agent + 文件路径查找（用于 upsert） */
    Optional<FimBaselineEntity> findByAgentIdAndFilePath(Long agentId, String filePath);

    /** 统计某 Agent 的基线条目数 */
    long countByAgentId(Long agentId);

    /** 删除某 Agent 的全部基线（重置用） */
    @Modifying
    @Query("DELETE FROM FimBaselineEntity e WHERE e.agentId = :agentId")
    void deleteByAgentId(@Param("agentId") Long agentId);
}
