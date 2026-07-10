package com.secguard.server.controller;

import com.secguard.common.dto.ApiResponse;
import com.secguard.common.enums.AlertStatus;
import com.secguard.common.enums.Severity;
import com.secguard.server.entity.Alert;
import com.secguard.server.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 告警管理控制器
 *
 * 提供告警查询、统计、状态变更等管理端点。
 * 所有端点需要 JWT 认证。
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /**
     * 分页查询告警列表
     * GET /api/alerts?severity=HIGH&status=OPEN&category=authentication&page=0&size=20
     */
    @GetMapping
    public ApiResponse<Page<Alert>> listAlerts(
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(alertService.queryAlerts(severity, status, category, page, size));
    }

    /**
     * 告警详情
     * GET /api/alerts/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<Alert> getAlert(@PathVariable Long id) {
        return ApiResponse.ok(alertService.getAlert(id));
    }

    /**
     * 告警统计
     * GET /api/alerts/stats
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(alertService.getStats());
    }

    /**
     * 确认告警
     * PUT /api/alerts/{id}/acknowledge
     */
    @PutMapping("/{id}/acknowledge")
    public ApiResponse<Alert> acknowledge(@PathVariable Long id) {
        return ApiResponse.ok(alertService.updateStatus(id, AlertStatus.ACKNOWLEDGED));
    }

    /**
     * 解决告警
     * PUT /api/alerts/{id}/resolve
     */
    @PutMapping("/{id}/resolve")
    public ApiResponse<Alert> resolve(@PathVariable Long id) {
        return ApiResponse.ok(alertService.updateStatus(id, AlertStatus.RESOLVED));
    }

    /**
     * 标记误报
     * PUT /api/alerts/{id}/false-positive
     */
    @PutMapping("/{id}/false-positive")
    public ApiResponse<Alert> markFalsePositive(@PathVariable Long id) {
        return ApiResponse.ok(alertService.updateStatus(id, AlertStatus.FALSE_POSITIVE));
    }
}
