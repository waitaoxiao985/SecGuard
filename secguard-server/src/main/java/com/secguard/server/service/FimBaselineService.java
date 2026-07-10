package com.secguard.server.service;

import com.secguard.common.dto.FIMBaselineEntry;
import com.secguard.common.dto.FIMBaselineSnapshot;
import com.secguard.server.entity.FimBaselineEntity;
import com.secguard.server.repository.AgentRepository;
import com.secguard.server.repository.FimBaselineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * FIM 基线管理服务
 *
 * 接收 Agent 上报的基线快照并持久化到 Server 端 sg_fim_baseline 表。
 * 支持基线查询、重置等操作。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FimBaselineService {

    private final FimBaselineRepository baselineRepository;
    private final AgentRepository agentRepository;

    /**
     * 接收 Agent 上报的基线快照（全量替换策略）
     *
     * 先删除该 Agent 的全部旧基线，再批量插入新快照。
     * Agent 每次上报的都是完整快照，因此 delete-then-insert 是最安全的策略。
     *
     * @param agentKey Agent 密钥
     * @param snapshot 基线快照
     * @return 实际存储的条目数
     */
    @Transactional
    public int ingestBaseline(String agentKey, FIMBaselineSnapshot snapshot) {
        Long agentId = agentRepository.findByAgentKey(agentKey)
                .map(a -> a.getId())
                .orElse(null);

        if (agentId == null) {
            log.warn("Received FIM baseline from unknown agent: {}", agentKey);
            return 0;
        }

        // 清除旧基线
        baselineRepository.deleteByAgentId(agentId);

        // 批量插入新基线
        int version = snapshot.getVersion();
        List<FimBaselineEntity> entities = snapshot.getEntries().stream()
                .map(entry -> toEntity(agentId, entry, version))
                .toList();
        baselineRepository.saveAll(entities);

        log.info("Ingested {} FIM baseline entries from agent {} (version={})",
                entities.size(), agentKey, version);

        return entities.size();
    }

    /**
     * 分页查询基线
     *
     * @param agentId 可选，按 Agent 过滤
     * @param page    页码
     * @param size    每页大小
     */
    public Page<FimBaselineEntity> queryBaseline(Long agentId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "filePath"));
        if (agentId != null) {
            return baselineRepository.findByAgentId(agentId, pageable);
        }
        return baselineRepository.findAll(pageable);
    }

    /**
     * 重置某 Agent 的基线（清空后等待 Agent 下次扫描自动上报）
     *
     * @param agentId Agent ID
     */
    @Transactional
    public void resetBaseline(Long agentId) {
        agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found: " + agentId));
        baselineRepository.deleteByAgentId(agentId);
        log.info("Reset FIM baseline for agent {}", agentId);
    }

    /**
     * 统计某 Agent 的基线条目数
     */
    public long countByAgentId(Long agentId) {
        return baselineRepository.countByAgentId(agentId);
    }

    private FimBaselineEntity toEntity(Long agentId, FIMBaselineEntry entry, int version) {
        return FimBaselineEntity.builder()
                .agentId(agentId)
                .filePath(entry.getFilePath())
                .sha256Hash(entry.getSha256())
                .fileSize(entry.getFileSize())
                .permissions(entry.getPermissions())
                .owner(entry.getOwner())
                .baselineVersion(version)
                .build();
    }
}
