package com.secguard.server.repository;

import com.secguard.server.entity.AssetPortEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 开放端口 Repository
 */
@Repository
public interface AssetPortRepository extends JpaRepository<AssetPortEntity, Long> {

    /** 分页查询某 Agent 的端口列表 */
    Page<AssetPortEntity> findByAgentId(Long agentId, Pageable pageable);

    /** 获取某 Agent 所有端口（不分页） */
    List<AssetPortEntity> findByAgentId(Long agentId);

    /** 按端口号搜索（跨 Agent，用于排查某端口被哪些主机监听） */
    List<AssetPortEntity> findByLocalPort(Integer localPort);

    /** 统计某 Agent 的端口数 */
    long countByAgentId(Long agentId);

    /** 删除某 Agent 的全部端口记录（全量替换时调用） */
    @Modifying
    @Query("DELETE FROM AssetPortEntity e WHERE e.agentId = :agentId")
    void deleteByAgentId(@Param("agentId") Long agentId);
}
