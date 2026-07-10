package com.secguard.server.controller;

import com.secguard.common.dto.ApiResponse;
import com.secguard.common.dto.FIMEvent;
import com.secguard.server.entity.FimEventEntity;
import com.secguard.server.service.FimEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * FIM 事件控制器
 *
 * Agent 端点：接收批量 FIM 事件（X-Agent-Key 认证）
 * 管理端点：分页查询 FIM 事件（JWT 认证）
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class FimEventController {

    private final FimEventService fimEventService;

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
     * 分页查询 FIM 事件（管理端）
     * GET /api/events/fim?page=0&size=20&agentId=1&eventType=MODIFIED
     */
    @GetMapping("/fim")
    public ApiResponse<Page<FimEventEntity>> queryFimEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String eventType) {

        return ApiResponse.ok(fimEventService.queryEvents(agentId, eventType, page, size));
    }
}
