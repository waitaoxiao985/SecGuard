package com.secguard.server.service;

import com.secguard.common.dto.FIMEvent;
import com.secguard.server.config.FimAlertProperties;
import com.secguard.server.engine.model.SecurityEvent;
import com.secguard.server.entity.FimEventEntity;
import com.secguard.server.repository.AgentRepository;
import com.secguard.server.repository.FimEventRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * FIM 事件服务
 *
 * 接收 Agent 上报的文件完整性变更事件并持久化到 sg_fim_event 表。
 * W6 增强：
 * - JPA Specification 动态条件查询（时间范围、路径模糊匹配）
 * - 统计 API（按类型/Agent 分组、趋势、TOP 路径）
 * - 高危路径变更自动产生告警
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FimEventService {

    private final FimEventRepository fimEventRepository;
    private final AgentRepository agentRepository;
    private final AlertService alertService;
    private final FimAlertProperties fimAlertProperties;
    private final TransactionTemplate transactionTemplate;

    /**
     * 批量接收并持久化 FIM 事件（含告警集成）
     *
     * 1. 在独立事务中持久化事件
     * 2. 检查高危路径变更并触发告警
     *
     * @param agentKey Agent 密钥
     * @param events   FIM 事件列表
     * @return 实际存储的事件数
     */
    public int ingestEvents(String agentKey, List<FIMEvent> events) {
        Long agentId = agentRepository.findByAgentKey(agentKey)
                .map(a -> a.getId())
                .orElse(null);

        if (agentId == null) {
            log.warn("Received FIM events from unknown agent: {}", agentKey);
            return 0;
        }

        List<FimEventEntity> entities = events.stream()
                .map(event -> toEntity(agentId, event))
                .toList();

        // 1. 在独立事务中持久化（与 LogEventService 一致）
        transactionTemplate.executeWithoutResult(status -> {
            fimEventRepository.saveAll(entities);
        });
        log.info("Ingested {} FIM events from agent {} (id={})", events.size(), agentKey, agentId);

        // 2. 检查高危路径并生成告警
        for (int i = 0; i < entities.size(); i++) {
            checkAndAlert(entities.get(i), agentId);
        }

        return entities.size();
    }

    /**
     * 增强分页查询 FIM 事件（JPA Specification 动态条件）
     *
     * @param agentId     可选，按 Agent 过滤
     * @param eventType   可选，按事件类型过滤 (ADDED/MODIFIED/DELETED)
     * @param startTime   可选，时间范围起始
     * @param endTime     可选，时间范围结束
     * @param pathPattern 可选，文件路径模糊匹配
     * @param page        页码
     * @param size        每页大小
     */
    public Page<FimEventEntity> queryEvents(Long agentId, String eventType,
                                             LocalDateTime startTime, LocalDateTime endTime,
                                             String pathPattern, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<FimEventEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (agentId != null) {
                predicates.add(cb.equal(root.get("agentId"), agentId));
            }
            if (eventType != null && !eventType.isBlank()) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endTime));
            }
            if (pathPattern != null && !pathPattern.isBlank()) {
                predicates.add(cb.like(root.get("filePath"), "%" + pathPattern + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return fimEventRepository.findAll(spec, pageable);
    }

    /**
     * FIM 事件统计
     *
     * @param trendDays 趋势天数（默认 7）
     */
    public Map<String, Object> getStats(int trendDays) {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 总量
        stats.put("total", fimEventRepository.count());

        // 最近 24h
        stats.put("last24h", fimEventRepository.countByCreatedAtAfter(LocalDateTime.now().minusHours(24)));

        // 按事件类型分组
        Map<String, Long> byEventType = new LinkedHashMap<>();
        for (Object[] row : fimEventRepository.countByEventTypeGrouped()) {
            byEventType.put((String) row[0], (Long) row[1]);
        }
        stats.put("byEventType", byEventType);

        // 按 Agent 分组
        Map<String, Long> byAgent = new LinkedHashMap<>();
        for (Object[] row : fimEventRepository.countByAgentIdGrouped()) {
            byAgent.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("byAgent", byAgent);

        // 最近 N 天趋势
        LocalDateTime since = LocalDateTime.now().minusDays(trendDays);
        List<Map<String, Object>> trend = new ArrayList<>();
        for (Object[] row : fimEventRepository.countByDaySince(since)) {
            trend.add(Map.of("date", row[0].toString(), "count", row[1]));
        }
        stats.put("trend", trend);

        // TOP 10 活跃文件路径
        List<Map<String, Object>> topPaths = new ArrayList<>();
        for (Object[] row : fimEventRepository.findTopFilePaths(PageRequest.of(0, 10))) {
            topPaths.add(Map.of("path", row[0], "count", row[1]));
        }
        stats.put("topPaths", topPaths);

        return stats;
    }

    /**
     * 检查 FIM 事件是否匹配高危路径，匹配则产生告警
     */
    private void checkAndAlert(FimEventEntity entity, Long agentId) {
        if (entity.getFilePath() == null) return;

        for (FimAlertProperties.HighRiskPath riskPath : fimAlertProperties.getHighRiskPaths()) {
            if (entity.getFilePath().contains(riskPath.getPattern())) {
                SecurityEvent securityEvent = SecurityEvent.builder()
                        .ruleId(90000 + fimAlertProperties.getHighRiskPaths().indexOf(riskPath))
                        .ruleName("FIM: " + riskPath.getDescription())
                        .ruleLevel(riskPath.getSeverity())
                        .category("fim")
                        .description(String.format("[%s] %s on %s (agent %d)",
                                entity.getEventType(), riskPath.getDescription(),
                                entity.getFilePath(), agentId))
                        .mitreTactic(riskPath.getMitreTactic())
                        .mitreTechnique(riskPath.getMitreTechnique())
                        .agentId(agentId)
                        .eventFields(Map.of(
                                "file_path", entity.getFilePath(),
                                "event_type", entity.getEventType(),
                                "sha256", entity.getSha256Hash() != null ? entity.getSha256Hash() : "",
                                "previous_hash", entity.getPreviousHash() != null ? entity.getPreviousHash() : ""
                        ))
                        .source("fim")
                        .build();

                try {
                    alertService.createFromEvent(securityEvent);
                    log.info("FIM alert triggered: path={}, rule={}", entity.getFilePath(), riskPath.getDescription());
                } catch (Exception e) {
                    log.warn("Failed to create FIM alert: {}", e.getMessage());
                }
                break; // 一个事件只触发最匹配的一条规则
            }
        }
    }

    private FimEventEntity toEntity(Long agentId, FIMEvent event) {
        return FimEventEntity.builder()
                .agentId(agentId)
                .filePath(event.getFilePath())
                .eventType(event.getEventType().name())
                .sha256Hash(event.getSha256())
                .previousHash(event.getPreviousSha256())
                .fileSize(event.getFileSize())
                .permissions(event.getPermissions())
                .owner(event.getOwner())
                .build();
    }
}
