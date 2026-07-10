package com.secguard.server.repository;

import com.secguard.server.entity.AssetNetworkEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 网络接口 Repository
 */
@Repository
public interface AssetNetworkRepository extends JpaRepository<AssetNetworkEntity, Long> {

    /** 分页查询某 Agent 的网络接口 */
    Page<AssetNetworkEntity> findByAgentId(Long agentId, Pageable pageable);

    /** 获取某 Agent 所有网络接口（不分页） */
    List<AssetNetworkEntity> findByAgentId(Long agentId);

    /** 按 IP 搜索（跨 Agent） */
    List<AssetNetworkEntity> findByIpv4(String ipv4);

    /** 统计某 Agent 的网卡数 */
    long countByAgentId(Long agentId);

    /** 删除某 Agent 的全部网络接口记录（全量替换时调用） */
    @Modifying
    @Query("DELETE FROM AssetNetworkEntity e WHERE e.agentId = :agentId")
    void deleteByAgentId(@Param("agentId") Long agentId);
}
