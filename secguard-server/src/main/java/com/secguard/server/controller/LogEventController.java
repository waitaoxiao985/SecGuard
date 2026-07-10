package com.secguard.server.controller;

import com.secguard.common.dto.ApiResponse;
import com.secguard.common.dto.LogEvent;
import com.secguard.server.service.LogEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 日志事件控制器
 *
 * Agent 端点：接收批量日志事件（X-Agent-Key 认证）
 * 管理端点：分页查询日志事件（JWT 认证，W5+ 使用）
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class LogEventController {

    private final LogEventService logEventService;

    /**
     * 接收 Agent 批量上报的日志事件
     * POST /api/events/logs
     * 认证方式：X-Agent-Key（ROLE_AGENT）
     */
    @PostMapping("/logs")
    public ApiResponse<Map<String, Object>> ingestLogs(
            @RequestHeader("X-Agent-Key") String agentKey,
            @RequestBody List<LogEvent> events) {

        int count = logEventService.ingestEvents(agentKey, events);

        return ApiResponse.ok(Map.of(
                "accepted", count,
                "total", events.size()
        ));
    }

    /**
     * 分页查询日志事件（管理端）
     * GET /api/events/logs?page=0&size=20&agentId=1&category=AUTHENTICATION
     */
    @GetMapping("/logs")
    public ApiResponse<Page<?>> queryLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String category) {

        return ApiResponse.ok(logEventService.queryLogs(agentId, category, page, size));
    }

    /**
     * 日志统计
     * GET /api/events/stats
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(Map.of(
                "totalEvents", logEventService.countTotal()
        ));
    }
}
