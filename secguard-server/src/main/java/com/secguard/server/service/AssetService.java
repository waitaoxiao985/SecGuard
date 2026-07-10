package com.secguard.server.service;

import com.secguard.common.dto.*;
import com.secguard.server.entity.AssetNetworkEntity;
import com.secguard.server.entity.AssetPortEntity;
import com.secguard.server.entity.AssetSoftwareEntity;
import com.secguard.server.entity.AssetSystemEntity;
import com.secguard.server.repository.AgentRepository;
import com.secguard.server.repository.AssetNetworkRepository;
import com.secguard.server.repository.AssetPortRepository;
import com.secguard.server.repository.AssetSoftwareRepository;
import com.secguard.server.repository.AssetSystemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主机资产管理服务
 *
 * 接收 Agent 上报的资产快照（系统信息 + 软件 + 端口 + 网络接口），
 * 持久化到 sg_asset_* 四张表，并提供查询和统计 API。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssetService {

    private final AgentRepository agentRepository;
    private final AssetSystemRepository systemRepository;
    private final AssetSoftwareRepository softwareRepository;
    private final AssetPortRepository portRepository;
    private final AssetNetworkRepository networkRepository;

    // ==================== 数据接收 ====================

    /**
     * 接收 Agent 上报的资产快照
     *
     * 系统信息: 直接插入历史记录
     * 软件/端口/网络接口: delete-then-insert 全量替换
     */
    @Transactional
    public Map<String, Object> ingestInventory(String agentKey, InventorySnapshot snapshot) {
        Long agentId = agentRepository.findByAgentKey(agentKey)
                .map(a -> a.getId())
                .orElse(null);

        if (agentId == null) {
            log.warn("Received inventory from unknown agent: {}", agentKey);
            return Map.of("accepted", false, "reason", "unknown agent");
        }

        LocalDateTime collectedAt = snapshot.getCollectedAt() != null
                ? LocalDateTime.ofInstant(snapshot.getCollectedAt(), ZoneOffset.systemDefault())
                : LocalDateTime.now();

        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 系统信息（追加历史记录）
        if (snapshot.getSystem() != null) {
            AssetSystemEntity sysEntity = toSystemEntity(agentId, snapshot.getSystem(), collectedAt);
            systemRepository.save(sysEntity);
            result.put("system", 1);
        }

        // 2. 软件列表（全量替换）
        softwareRepository.deleteByAgentId(agentId);
        if (snapshot.getSoftware() != null && !snapshot.getSoftware().isEmpty()) {
            List<AssetSoftwareEntity> entities = snapshot.getSoftware().stream()
                    .map(s -> toSoftwareEntity(agentId, s, collectedAt))
                    .toList();
            softwareRepository.saveAll(entities);
            result.put("software", entities.size());
        } else {
            result.put("software", 0);
        }

        // 3. 端口列表（全量替换）
        portRepository.deleteByAgentId(agentId);
        if (snapshot.getPorts() != null && !snapshot.getPorts().isEmpty()) {
            List<AssetPortEntity> entities = snapshot.getPorts().stream()
                    .map(p -> toPortEntity(agentId, p, collectedAt))
                    .toList();
            portRepository.saveAll(entities);
            result.put("ports", entities.size());
        } else {
            result.put("ports", 0);
        }

        // 4. 网络接口（全量替换）
        networkRepository.deleteByAgentId(agentId);
        if (snapshot.getNetworks() != null && !snapshot.getNetworks().isEmpty()) {
            List<AssetNetworkEntity> entities = snapshot.getNetworks().stream()
                    .map(n -> toNetworkEntity(agentId, n, collectedAt))
                    .toList();
            networkRepository.saveAll(entities);
            result.put("networks", entities.size());
        } else {
            result.put("networks", 0);
        }

        log.info("Ingested inventory from agent {} (id={}): {}", agentKey, agentId, result);
        result.put("accepted", true);
        return result;
    }

    // ==================== 查询 API ====================

    /**
     * 查询某 Agent 的最新系统信息
     */
    public AssetSystemEntity getLatestSystemInfo(Long agentId) {
        return systemRepository.findTopByAgentIdOrderByCollectedAtDesc(agentId).orElse(null);
    }

    /**
     * 分页查询系统信息历史
     */
    public Page<AssetSystemEntity> querySystemHistory(Long agentId, int page, int size) {
        return systemRepository.findByAgentIdOrderByCollectedAtDesc(
                agentId, PageRequest.of(page, size));
    }

    /**
     * 分页查询软件列表
     */
    public Page<AssetSoftwareEntity> querySoftware(Long agentId, int page, int size) {
        return softwareRepository.findByAgentId(
                agentId, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name")));
    }

    /**
     * 分页查询端口列表
     */
    public Page<AssetPortEntity> queryPorts(Long agentId, int page, int size) {
        return portRepository.findByAgentId(
                agentId, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "localPort")));
    }

    /**
     * 分页查询网络接口
     */
    public Page<AssetNetworkEntity> queryNetworks(Long agentId, int page, int size) {
        return networkRepository.findByAgentId(
                agentId, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "interfaceName")));
    }

    /**
     * 按端口号搜索（跨 Agent）
     */
    public List<AssetPortEntity> searchByPort(int port) {
        return portRepository.findByLocalPort(port);
    }

    /**
     * 按软件名搜索（跨 Agent）
     */
    public Page<AssetSoftwareEntity> searchSoftware(String keyword, int page, int size) {
        return softwareRepository.findByNameContainingIgnoreCase(
                keyword, PageRequest.of(page, size));
    }

    // ==================== 统计 ====================

    /**
     * 资产统计摘要
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("agentsWithInventory", systemRepository.countDistinctAgents());
        stats.put("totalSoftware", softwareRepository.count());
        stats.put("totalPorts", portRepository.count());
        stats.put("totalNetworks", networkRepository.count());
        return stats;
    }

    /**
     * 某 Agent 的资产概要
     */
    public Map<String, Object> getAgentInventorySummary(Long agentId) {
        Map<String, Object> summary = new LinkedHashMap<>();

        // 最新系统信息
        systemRepository.findTopByAgentIdOrderByCollectedAtDesc(agentId)
                .ifPresent(sys -> {
                    summary.put("hostname", sys.getHostname());
                    summary.put("os", sys.getOs());
                    summary.put("osVersion", sys.getOsVersion());
                    summary.put("kernel", sys.getKernel());
                    summary.put("cpuModel", sys.getCpuModel());
                    summary.put("cpuCores", sys.getCpuCores());
                    summary.put("ramTotalMb", sys.getRamTotalMb());
                    summary.put("collectedAt", sys.getCollectedAt());
                });

        summary.put("softwareCount", softwareRepository.countByAgentId(agentId));
        summary.put("portCount", portRepository.countByAgentId(agentId));
        summary.put("networkCount", networkRepository.countByAgentId(agentId));

        return summary;
    }

    // ==================== 转换方法 ====================

    private AssetSystemEntity toSystemEntity(Long agentId, AssetSystemDTO dto, LocalDateTime collectedAt) {
        return AssetSystemEntity.builder()
                .agentId(agentId)
                .hostname(dto.getHostname())
                .os(dto.getOs())
                .osVersion(dto.getOsVersion())
                .kernel(dto.getKernel())
                .cpuModel(dto.getCpuModel())
                .cpuCores(dto.getCpuCores())
                .ramTotalMb(dto.getRamTotalMb())
                .uptimeSeconds(dto.getUptimeSeconds())
                .collectedAt(collectedAt)
                .build();
    }

    private AssetSoftwareEntity toSoftwareEntity(Long agentId, AssetSoftwareDTO dto, LocalDateTime collectedAt) {
        return AssetSoftwareEntity.builder()
                .agentId(agentId)
                .name(dto.getName())
                .version(dto.getVersion())
                .vendor(dto.getVendor())
                .format(dto.getFormat())
                .collectedAt(collectedAt)
                .build();
    }

    private AssetPortEntity toPortEntity(Long agentId, AssetPortDTO dto, LocalDateTime collectedAt) {
        return AssetPortEntity.builder()
                .agentId(agentId)
                .protocol(dto.getProtocol())
                .localPort(dto.getLocalPort())
                .state(dto.getState())
                .processName(dto.getProcessName())
                .processPid(dto.getProcessPid())
                .collectedAt(collectedAt)
                .build();
    }

    private AssetNetworkEntity toNetworkEntity(Long agentId, AssetNetworkDTO dto, LocalDateTime collectedAt) {
        return AssetNetworkEntity.builder()
                .agentId(agentId)
                .interfaceName(dto.getInterfaceName())
                .macAddress(dto.getMacAddress())
                .ipv4(dto.getIpv4())
                .ipv6(dto.getIpv6())
                .gateway(dto.getGateway())
                .dns(dto.getDns())
                .collectedAt(collectedAt)
                .build();
    }
}
