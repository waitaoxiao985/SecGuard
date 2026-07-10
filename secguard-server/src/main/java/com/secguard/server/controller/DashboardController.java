package com.secguard.server.controller;

import com.secguard.common.dto.ApiResponse;
import com.secguard.common.enums.AlertStatus;
import com.secguard.server.repository.*;
import com.secguard.server.service.AlertService;
import com.secguard.server.service.AssetService;
import com.secguard.server.service.FimEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Dashboard 聚合统计控制器
 *
 * 为前端 Dashboard 提供跨模块的汇总数据。
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final AgentRepository agentRepository;
    private final AlertRepository alertRepository;
    private final VulnerabilityRepository vulnRepository;
    private final AssetService assetService;
    private final FimEventService fimEventService;

    /**
     * Agent 统计
     * GET /api/agents/stats
     */
    @GetMapping("/api/agents/stats")
    public ApiResponse<Map<String, Object>> agentStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", agentRepository.count());

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : agentRepository.countByStatusGrouped()) {
            byStatus.put(row[0].toString(), (Long) row[1]);
        }
        stats.put("byStatus", byStatus);
        return ApiResponse.ok(stats);
    }

    /**
     * 告警 24h 趋势（按小时分组）
     * GET /api/alerts/trend
     */
    @GetMapping("/api/alerts/trend")
    public ApiResponse<List<Map<String, Object>>> alertTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int h = 23; h >= 0; h--) {
            LocalDateTime hourStart = now.minusHours(h).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime hourEnd = hourStart.plusHours(1);
            String label = String.format("%02d:00", hourStart.getHour());

            long count = alertRepository.count(new org.springframework.data.jpa.domain.Specification<com.secguard.server.entity.Alert>() {
                @Override
                public jakarta.persistence.criteria.Predicate toPredicate(
                        jakarta.persistence.criteria.Root<com.secguard.server.entity.Alert> root,
                        jakarta.persistence.criteria.CriteriaQuery<?> query,
                        jakarta.persistence.criteria.CriteriaBuilder cb) {
                    return cb.and(
                            cb.greaterThanOrEqualTo(root.get("createdAt"), hourStart),
                            cb.lessThan(root.get("createdAt"), hourEnd)
                    );
                }
            });

            trend.add(Map.of("hour", label, "count", count));
        }

        return ApiResponse.ok(trend);
    }
}
