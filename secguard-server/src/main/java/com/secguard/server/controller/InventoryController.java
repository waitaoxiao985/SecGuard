package com.secguard.server.controller;

import com.secguard.common.dto.ApiResponse;
import com.secguard.common.dto.InventorySnapshot;
import com.secguard.server.entity.AssetNetworkEntity;
import com.secguard.server.entity.AssetPortEntity;
import com.secguard.server.entity.AssetSoftwareEntity;
import com.secguard.server.entity.AssetSystemEntity;
import com.secguard.server.service.AssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 主机资产控制器
 *
 * Agent 端点：接收资产快照上报（X-Agent-Key 认证）
 * 管理端点：资产查询/统计/搜索（JWT 认证）
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final AssetService assetService;

    // ===== Agent 上报端点 =====

    /**
     * 接收 Agent 上报的资产快照
     * POST /api/events/inventory
     * 认证方式：X-Agent-Key（ROLE_AGENT）
     */
    @PostMapping("/api/events/inventory")
    public ApiResponse<Map<String, Object>> ingestInventory(
            @RequestHeader("X-Agent-Key") String agentKey,
            @RequestBody InventorySnapshot snapshot) {
        Map<String, Object> result = assetService.ingestInventory(agentKey, snapshot);
        return ApiResponse.ok(result);
    }

    // ===== 管理端查询端点 =====

    /**
     * 资产统计摘要
     * GET /api/inventory/stats
     * 认证方式：JWT
     */
    @GetMapping("/api/inventory/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(assetService.getStats());
    }

    /**
     * 某 Agent 的资产概要
     * GET /api/inventory/summary/{agentId}
     */
    @GetMapping("/api/inventory/summary/{agentId}")
    public ApiResponse<Map<String, Object>> agentSummary(@PathVariable Long agentId) {
        return ApiResponse.ok(assetService.getAgentInventorySummary(agentId));
    }

    /**
     * 查询最新系统信息
     * GET /api/inventory/system?agentId=1
     */
    @GetMapping("/api/inventory/system")
    public ApiResponse<AssetSystemEntity> latestSystem(@RequestParam Long agentId) {
        AssetSystemEntity sys = assetService.getLatestSystemInfo(agentId);
        return ApiResponse.ok(sys);
    }

    /**
     * 系统信息历史（分页）
     * GET /api/inventory/system/history?agentId=1&page=0&size=10
     */
    @GetMapping("/api/inventory/system/history")
    public ApiResponse<Page<AssetSystemEntity>> systemHistory(
            @RequestParam Long agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(assetService.querySystemHistory(agentId, page, size));
    }

    /**
     * 查询软件列表（分页）
     * GET /api/inventory/software?agentId=1&page=0&size=20
     */
    @GetMapping("/api/inventory/software")
    public ApiResponse<Page<AssetSoftwareEntity>> querySoftware(
            @RequestParam Long agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(assetService.querySoftware(agentId, page, size));
    }

    /**
     * 搜索软件（跨 Agent，按名称模糊匹配）
     * GET /api/inventory/software/search?keyword=java&page=0&size=20
     */
    @GetMapping("/api/inventory/software/search")
    public ApiResponse<Page<AssetSoftwareEntity>> searchSoftware(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(assetService.searchSoftware(keyword, page, size));
    }

    /**
     * 查询端口列表（分页）
     * GET /api/inventory/ports?agentId=1&page=0&size=20
     */
    @GetMapping("/api/inventory/ports")
    public ApiResponse<Page<AssetPortEntity>> queryPorts(
            @RequestParam Long agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(assetService.queryPorts(agentId, page, size));
    }

    /**
     * 按端口号搜索（跨 Agent）
     * GET /api/inventory/ports/search?port=22
     */
    @GetMapping("/api/inventory/ports/search")
    public ApiResponse<List<AssetPortEntity>> searchByPort(@RequestParam int port) {
        return ApiResponse.ok(assetService.searchByPort(port));
    }

    /**
     * 查询网络接口（分页）
     * GET /api/inventory/networks?agentId=1&page=0&size=20
     */
    @GetMapping("/api/inventory/networks")
    public ApiResponse<Page<AssetNetworkEntity>> queryNetworks(
            @RequestParam Long agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(assetService.queryNetworks(agentId, page, size));
    }
}
