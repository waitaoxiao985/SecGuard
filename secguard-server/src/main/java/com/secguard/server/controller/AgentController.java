package com.secguard.server.controller;

import com.secguard.common.dto.*;
import com.secguard.common.enums.AgentStatus;
import com.secguard.server.dto.AgentDetailResponse;
import com.secguard.server.entity.Agent;
import com.secguard.server.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 管理 API
 *
 * === Agent 通信（需 X-Agent-Key 或公开） ===
 * POST /api/agents/register    - Agent 注册（公开）
 * POST /api/agents/heartbeat   - Agent 心跳（X-Agent-Key）
 *
 * === 管理端（需 JWT 认证） ===
 * GET    /api/agents            - Agent 分页列表
 * GET    /api/agents/{id}       - Agent 详情（含配置）
 * PUT    /api/agents/{id}/disable - 禁用 Agent
 * PUT    /api/agents/{id}/enable  - 启用 Agent
 * DELETE /api/agents/{id}       - 删除 Agent
 * GET    /api/agents/stats      - Agent 状态统计
 */
@RestController
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    // ==================== Agent 通信 ====================

    /**
     * Agent 注册（公开接口）
     * 支持去重：同 IP+hostname 的 Agent 重复注册时返回已有 key
     */
    @PostMapping("/api/agents/register")
    public ApiResponse<AgentRegisterResponse> register(
            @Valid @RequestBody AgentRegisterRequest request) {
        return ApiResponse.ok(agentService.register(request));
    }

    /**
     * Agent 心跳（需 X-Agent-Key header）
     * 返回最新配置用于配置下发
     */
    @PostMapping("/api/agents/heartbeat")
    public ApiResponse<AgentHeartbeatResponse> heartbeat(
            @RequestHeader("X-Agent-Key") String agentKey,
            @RequestBody AgentHeartbeatRequest request) {
        return ApiResponse.ok(agentService.heartbeat(agentKey, request));
    }

    // ==================== 管理端 ====================

    /**
     * Agent 分页列表
     * 支持参数：page(从0开始), size, status(ACTIVE/DISCONNECTED/PENDING/DISABLED), keyword
     */
    @GetMapping("/api/agents")
    public ApiResponse<Page<Agent>> listAgents(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) AgentStatus status,
            @RequestParam(required = false) String keyword) {

        AgentListQuery query = AgentListQuery.builder()
                .page(page)
                .size(size)
                .status(status)
                .keyword(keyword)
                .build();

        return ApiResponse.ok(agentService.listAgents(query));
    }

    /**
     * Agent 详情（含配置信息）
     */
    @GetMapping("/api/agents/{id}")
    public ApiResponse<AgentDetailResponse> getAgent(@PathVariable Long id) {
        return ApiResponse.ok(agentService.getAgentDetail(id));
    }

    /**
     * 禁用 Agent
     */
    @PutMapping("/api/agents/{id}/disable")
    public ApiResponse<Void> disableAgent(@PathVariable Long id) {
        agentService.disableAgent(id);
        return ApiResponse.ok();
    }

    /**
     * 启用 Agent
     */
    @PutMapping("/api/agents/{id}/enable")
    public ApiResponse<Void> enableAgent(@PathVariable Long id) {
        agentService.enableAgent(id);
        return ApiResponse.ok();
    }

    /**
     * 删除 Agent（硬删除，CASCADE 清理关联数据）
     */
    @DeleteMapping("/api/agents/{id}")
    public ApiResponse<Void> deleteAgent(@PathVariable Long id) {
        agentService.deleteAgent(id);
        return ApiResponse.ok();
    }
}
