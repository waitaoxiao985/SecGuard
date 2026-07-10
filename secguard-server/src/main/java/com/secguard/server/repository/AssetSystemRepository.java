package com.secguard.server.repository;

import com.secguard.server.entity.AssetSystemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 主机系统信息 Repository
 */
@Repository
public interface AssetSystemRepository extends JpaRepository<AssetSystemEntity, Long> {

    /** 查询某 Agent 的系统信息（按采集时间降序） */
    Page<AssetSystemEntity> findByAgentIdOrderByCollectedAtDesc(Long agentId, Pageable pageable);

    /** 获取某 Agent 最新一条系统信息 */
    Optional<AssetSystemEntity> findTopByAgentIdOrderByCollectedAtDesc(Long agentId);

    /** 统计有系统信息的 Agent 数量 */
    @Query("SELECT COUNT(DISTINCT e.agentId) FROM AssetSystemEntity e")
    long countDistinctAgents();

    /** 删除某 Agent 的历史系统信息（保留最新一条时调用） */
    @Modifying
    @Query("DELETE FROM AssetSystemEntity e WHERE e.agentId = :agentId")
    void deleteByAgentId(@Param("agentId") Long agentId);
}
