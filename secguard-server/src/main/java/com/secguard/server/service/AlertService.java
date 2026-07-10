package com.secguard.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secguard.common.enums.AlertStatus;
import com.secguard.common.enums.EventCategory;
import com.secguard.common.enums.Severity;
import com.secguard.server.engine.model.SecurityEvent;
import com.secguard.server.entity.Agent;
import com.secguard.server.entity.Alert;
import com.secguard.server.repository.AgentRepository;
import com.secguard.server.repository.AlertRepository;
import com.secguard.server.websocket.AlertWebSocketHandler;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警服务
 *
 * W4 核心：接收规则引擎产生的 SecurityEvent，创建告警记录并推送。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final AgentRepository agentRepository;
    private final AlertWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    /**
     * 从 SecurityEvent 创建告警
     */
    @Transactional
    public Alert createFromEvent(SecurityEvent event) {
        Agent agent = null;
        if (event.getAgentId() != null) {
            agent = agentRepository.findById(event.getAgentId()).orElse(null);
        }

        // 序列化 raw_event（含原始字段 + 提取字段）
        String rawEventJson = null;
        try {
            Map<String, Object> rawEvent = new LinkedHashMap<>();
            rawEvent.put("fields", event.getEventFields());
            rawEvent.put("extracted", event.getExtractedFields());
            rawEvent.put("rawLog", event.getRawLog());
            rawEvent.put("source", event.getSource());
            rawEventJson = objectMapper.writeValueAsString(rawEvent);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize raw event: {}", e.getMessage());
        }

        // 提取 source_ip
        String sourceIp = null;
        if (event.getExtractedFields() != null) {
            sourceIp = event.getExtractedFields().get("source_ip");
        }

        // 解析 category 枚举
        EventCategory category = EventCategory.SYSTEM;
        if (event.getCategory() != null) {
            try {
                category = EventCategory.valueOf(event.getCategory().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.debug("Unknown category: {}", event.getCategory());
            }
        }

        Alert alert = Alert.builder()
                .agent(agent)
                .ruleId((long) event.getRuleId())
                .ruleName(event.getRuleName())
                .severity(Severity.fromLevel(event.getRuleLevel()))
                .category(category)
                .description(event.getDescription())
                .mitreTactic(event.getMitreTactic())
                .mitreTechnique(event.getMitreTechnique())
                .rawEvent(rawEventJson)
                .sourceIp(sourceIp)
                .build();

        alert = alertRepository.save(alert);

        // WebSocket 推送
        try {
            webSocketHandler.broadcastAlert(alert);
        } catch (Exception e) {
            log.debug("WebSocket push failed (no connected clients): {}", e.getMessage());
        }

        log.info("Alert created: id={}, rule={}, severity={}, sourceIp={}",
                alert.getId(), alert.getRuleName(), alert.getSeverity(), sourceIp);

        return alert;
    }

    /**
     * 分页查询告警（使用 Specification 动态条件，避免 JPQL null 枚举 bug）
     */
    public Page<Alert> queryAlerts(Severity severity, AlertStatus status,
                                    String category, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Alert> spec = (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();
            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (category != null && !category.isEmpty()) {
                predicates.add(cb.equal(root.get("category"),
                        EventCategory.valueOf(category.toUpperCase())));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return alertRepository.findAll(spec, pageable);
    }

    /**
     * 告警详情
     */
    public Alert getAlert(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + id));
    }

    /**
     * 更新告警状态
     */
    @Transactional
    public Alert updateStatus(Long id, AlertStatus newStatus) {
        Alert alert = getAlert(id);
        alert.setStatus(newStatus);
        return alertRepository.save(alert);
    }

    /**
     * 告警统计
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", alertRepository.count());
        stats.put("open", alertRepository.countByStatus(AlertStatus.OPEN));
        stats.put("last24h", alertRepository.countByCreatedAtAfter(LocalDateTime.now().minusHours(24)));

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (Object[] row : alertRepository.countBySeverityGrouped()) {
            bySeverity.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("bySeverity", bySeverity);

        return stats;
    }
}
