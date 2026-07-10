package com.secguard.server.controller;

import com.secguard.common.dto.ApiResponse;
import com.secguard.common.dto.FIMBaselineSnapshot;
import com.secguard.common.dto.FIMEvent;
import com.secguard.server.entity.FimBaselineEntity;
import com.secguard.server.entity.FimEventEntity;
import com.secguard.server.service.FimBaselineService;
import com.secguard.server.service.FimEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * FIM 事件控制器
 *
 * Agent 端点：接收批量 FIM 事件和基线快照（X-Agent-Key 认证）
 * 管理端点：FIM 事件查询/统计 + 基线管理（JWT 认证）
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class FimEventController {

    private final FimEventService fimEventService;
    private final FimBaselineService fimBaselineService;

    // ===== Agent 上报端点 =====

    /**
     * 接收 Agent 批量上报的 FIM 事件
     * POST /api/events/fim
     * 认证方式：X-Agent-Key（ROLE_AGENT）
     */
    @PostMapping("/fim")
    public ApiResponse<Map<String, Object>> ingestFimEvents(
            @RequestHeader("X-Agent-Key") String agentKey,
            @RequestBody List<FIMEvent> events) {

        int count = fimEventService.ingestEvents(agentKey, events);

        return ApiResponse.ok(Map.of(
                "accepted", count,
                "total", events.size()
        ));
    }

    /**
     * Agent 上报基线快照
     * POST /api/events/fim/baseline
     * 认证方式：X-Agent-Key（ROLE_AGENT）
     */
    @PostMapping("/fim/baseline")
    public ApiResponse<Map<String, Object>> ingestBaseline(
            @RequestHeader("X-Agent-Key") String agentKey,
            @RequestBody FIMBaselineSnapshot snapshot) {
        int count = fimBaselineService.ingestBaseline(agentKey, snapshot);
        return ApiResponse.ok(Map.of("accepted", count));
    }

    // ===== 管理端查询端点 =====

    /**
     * FIM 事件统计
     * GET /api/events/fim/stats?trendDays=7
     * 认证方式：JWT
     */
    @GetMapping("/fim/stats")
    public ApiResponse<Map<String, Object>> stats(
            @RequestParam(defaultValue = "7") int trendDays) {
        return ApiResponse.ok(fimEventService.getStats(trendDays));
    }

    /**
     * 增强分页查询 FIM 事件（管理端）
     * GET /api/events/fim?page=0&size=20&agentId=1&eventType=MODIFIED
     *     &startTime=2026-04-01T00:00:00&endTime=2026-04-07T23:59:59&pathPattern=passwd
     */
    @GetMapping("/fim")
    public ApiResponse<Page<FimEventEntity>> queryFimEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String pathPattern) {

        return ApiResponse.ok(fimEventService.queryEvents(
                agentId, eventType, startTime, endTime, pathPattern, page, size));
    }

    // ===== 基线管理端点 =====

    /**
     * 查询基线（管理端）
     * GET /api/events/fim/baseline?agentId=1&page=0&size=50
     */
    @GetMapping("/fim/baseline")
    public ApiResponse<Page<FimBaselineEntity>> queryBaseline(
            @RequestParam(required = false) Long agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(fimBaselineService.queryBaseline(agentId, page, size));
    }

    /**
     * 重置 Agent 基线
     * PUT /api/events/fim/baseline/{agentId}/reset
     */
    @PutMapping("/fim/baseline/{agentId}/reset")
    public ApiResponse<Map<String, Object>> resetBaseline(@PathVariable Long agentId) {
        fimBaselineService.resetBaseline(agentId);
        return ApiResponse.ok(Map.of("reset", true, "agentId", agentId));
    }
}
