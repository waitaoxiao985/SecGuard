package com.secguard.server.repository;

import com.secguard.server.entity.AssetSoftwareEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 已安装软件 Repository
 */
@Repository
public interface AssetSoftwareRepository extends JpaRepository<AssetSoftwareEntity, Long> {

    /** 分页查询某 Agent 的软件列表 */
    Page<AssetSoftwareEntity> findByAgentId(Long agentId, Pageable pageable);

    /** 获取某 Agent 所有软件（不分页，用于导出/漏洞匹配） */
    List<AssetSoftwareEntity> findByAgentId(Long agentId);

    /** 统计某 Agent 的软件数量 */
    long countByAgentId(Long agentId);

    /** 按名称模糊搜索软件（跨 Agent） */
    Page<AssetSoftwareEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /** 删除某 Agent 的全部软件记录（全量替换时调用） */
    @Modifying
    @Query("DELETE FROM AssetSoftwareEntity e WHERE e.agentId = :agentId")
    void deleteByAgentId(@Param("agentId") Long agentId);
}
