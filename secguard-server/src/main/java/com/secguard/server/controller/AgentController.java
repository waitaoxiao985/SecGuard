package com.secguard.server.controller;

import com.secguard.common.dto.*;
import com.secguard.server.entity.Agent;
import com.secguard.server.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 通信 API
 *
 * POST /api/agents/register   - Agent 注册
 * POST /api/agents/heartbeat  - Agent 心跳（需 agent_key header）
 * GET  /api/agents            - Agent 列表（管理端）
 * GET  /api/agents/{id}       - Agent 详情（管理端）
 */
@RestController
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * Agent 注册（公开接口，无需认证）
     */
    @PostMapping("/api/agents/register")
    public ApiResponse<AgentRegisterResponse> register(@Valid @RequestBody AgentRegisterRequest request) {
        AgentRegisterResponse response = agentService.register(request);
        return ApiResponse.ok(response);
    }

    /**
     * Agent 心跳（需 X-Agent-Key header）
     */
    @PostMapping("/api/agents/heartbeat")
    public ApiResponse<Void> heartbeat(
            @RequestHeader("X-Agent-Key") String agentKey,
            @RequestBody AgentHeartbeatRequest request) {
        agentService.heartbeat(agentKey, request);
        return ApiResponse.ok();
    }

    /**
     * Agent 列表（管理端，需 JWT 认证）
     */
    @GetMapping("/api/agents")
    public ApiResponse<List<Agent>> listAgents() {
        return ApiResponse.ok(agentService.listAgents());
    }

    /**
     * Agent 详情（管理端，需 JWT 认证）
     */
    @GetMapping("/api/agents/{id}")
    public ApiResponse<Agent> getAgent(@PathVariable Long id) {
        return ApiResponse.ok(agentService.getAgent(id));
    }
}
