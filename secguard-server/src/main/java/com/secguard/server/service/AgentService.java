package com.secguard.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secguard.common.dto.*;
import com.secguard.common.enums.AgentStatus;
import com.secguard.server.dto.AgentDetailResponse;
import com.secguard.server.entity.Agent;
import com.secguard.server.entity.AgentConfig;
import com.secguard.server.repository.AgentConfigRepository;
import com.secguard.server.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Agent 管理服务
 *
 * W1: 注册、心跳、基础查询
 * W2: 完整生命周期管理、配置下发、分页/过滤/搜索、禁用/启用/删除
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private final AgentRepository agentRepository;
    private final AgentConfigRepository agentConfigRepository;
    private final ObjectMapper objectMapper;

    @Value("${secguard.agent.heartbeat-timeout:120}")
    private int heartbeatTimeout;

    // ==================== 注册 ====================

    /**
     * Agent 注册
     * - 检查 IP+hostname 是否已存在（去重）
     * - 生成唯一 agentKey
     * - 自动创建默认配置
     */
    @Transactional
    public AgentRegisterResponse register(AgentRegisterRequest request) {
        // 去重：同 IP + 同 hostname 的 Agent 如果存在且未禁用，直接返回已有 key
        if (request.getHostname() != null) {
            Optional<Agent> existing = agentRepository.findByIpAndHostname(
                    request.getIp(), request.getHostname());
            if (existing.isPresent() && existing.get().getStatus() != AgentStatus.DISABLED) {
                Agent agent = existing.get();
                agent.setStatus(AgentStatus.ACTIVE);
                agent.setLastKeepalive(LocalDateTime.now());
                agentRepository.save(agent);
                log.info("Agent re-registered (existing): id={}, name={}", agent.getId(), agent.getName());

                AgentConfig config = getOrCreateConfig(agent);

                return AgentRegisterResponse.builder()
                        .agentId(agent.getId())
                        .agentKey(agent.getAgentKey())
                        .config(toConfigDTO(config))
                        .build();
            }
        }

        // 新注册
        String agentKey = UUID.randomUUID().toString().replace("-", "");

        Agent agent = Agent.builder()
                .agentKey(agentKey)
                .name(request.getName())
                .ip(request.getIp())
                .os(request.getOs())
                .osVersion(request.getOsVersion())
                .hostname(request.getHostname())
                .agentVersion(request.getAgentVersion())
                .status(AgentStatus.ACTIVE)
                .lastKeepalive(LocalDateTime.now())
                .build();

        agent = agentRepository.save(agent);

        // 自动创建默认配置
        AgentConfig config = createDefaultConfig(agent);

        log.info("Agent registered: id={}, name={}, ip={}, key={}",
                agent.getId(), agent.getName(), agent.getIp(), agentKey);

        return AgentRegisterResponse.builder()
                .agentId(agent.getId())
                .agentKey(agentKey)
                .config(toConfigDTO(config))
                .build();
    }

    // ==================== 心跳 ====================

    /**
     * Agent 心跳
     * - 更新心跳时间和系统指标
     * - 自动恢复离线状态为 ACTIVE
     * - 返回配置（用于配置下发）
     */
    @Transactional
    public AgentHeartbeatResponse heartbeat(String agentKey, AgentHeartbeatRequest request) {
        Agent agent = agentRepository.findByAgentKey(agentKey)
                .orElseThrow(() -> new RuntimeException("Agent not found: " + agentKey));

        AgentStatus previousStatus = agent.getStatus();

        agent.setLastKeepalive(LocalDateTime.now());
        agent.setStatus(AgentStatus.ACTIVE);
        agent.setCpuUsage(request.getCpuUsage());
        agent.setMemUsage(request.getMemUsage());
        agent.setPid(request.getPid());
        agent.setUptime(request.getUptime());
        agentRepository.save(agent);

        // 如果从离线恢复，记录日志
        if (previousStatus == AgentStatus.DISCONNECTED) {
            log.info("Agent reconnected: id={}, name={}", agent.getId(), agent.getName());
        } else {
            log.debug("Agent heartbeat: key={}, cpu={}, mem={}",
                    agentKey, request.getCpuUsage(), request.getMemUsage());
        }

        // 返回配置（Agent 可对比本地配置判断是否需要更新）
        AgentConfig config = agentConfigRepository.findByAgentId(agent.getId())
                .orElse(null);

        return AgentHeartbeatResponse.builder()
                .config(config != null ? toConfigDTO(config) : null)
                .build();
    }

    // ==================== 列表与查询 ====================

    /**
     * 分页查询 Agent 列表
     * 支持按状态过滤、关键词搜索（名称/IP/主机名）
     */
    public Page<Agent> listAgents(AgentListQuery query) {
        int page = query.getPage() != null ? query.getPage() : 0;
        int size = query.getSize() != null ? query.getSize() : 20;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastKeepalive"));

        if (query.getStatus() != null && query.getKeyword() != null && !query.getKeyword().isBlank()) {
            return agentRepository.findByStatusAndKeyword(
                    query.getStatus(), query.getKeyword().trim(), pageable);
        } else if (query.getStatus() != null) {
            return agentRepository.findByStatus(query.getStatus(), pageable);
        } else if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            return agentRepository.findByKeyword(query.getKeyword().trim(), pageable);
        }
        return agentRepository.findAll(pageable);
    }

    /**
     * Agent 详情（含配置信息）
     */
    public AgentDetailResponse getAgentDetail(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent not found: " + id));

        AgentConfig config = agentConfigRepository.findByAgentId(id).orElse(null);

        return AgentDetailResponse.builder()
                .agent(agent)
                .config(config != null ? toConfigDTO(config) : null)
                .build();
    }

    // ==================== 状态管理 ====================

    /**
     * 禁用 Agent（软删除）
     */
    @Transactional
    public void disableAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent not found: " + id));
        agent.setStatus(AgentStatus.DISABLED);
        agentRepository.save(agent);
        log.info("Agent disabled: id={}, name={}", id, agent.getName());
    }

    /**
     * 启用 Agent
     */
    @Transactional
    public void enableAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent not found: " + id));
        agent.setStatus(AgentStatus.PENDING);
        agentRepository.save(agent);
        log.info("Agent enabled (pending): id={}, name={}", id, agent.getName());
    }

    /**
     * 删除 Agent（硬删除，CASCADE 清理关联数据）
     */
    @Transactional
    public void deleteAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent not found: " + id));
        agentRepository.delete(agent);
        log.info("Agent deleted: id={}, name={}", id, agent.getName());
    }

    // ==================== 离线检测 ====================

    /**
     * 检查并标记超时 Agent 为离线
     * 由 AgentHealthChecker 定时任务调用
     */
    @Transactional
    public int markDisconnectedAgents() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(heartbeatTimeout);
        List<Agent> staleAgents = agentRepository.findStaleActiveAgents(threshold);

        if (staleAgents.isEmpty()) return 0;

        for (Agent agent : staleAgents) {
            agent.setStatus(AgentStatus.DISCONNECTED);
            log.warn("Agent marked DISCONNECTED: id={}, name={}, lastKeepalive={}",
                    agent.getId(), agent.getName(), agent.getLastKeepalive());
        }
        agentRepository.saveAll(staleAgents);
        return staleAgents.size();
    }

    // ==================== 内部工具 ====================

    /**
     * 根据 agentKey 验证 Agent 身份
     */
    public boolean validateAgentKey(String agentKey) {
        return agentRepository.findByAgentKey(agentKey)
                .map(a -> a.getStatus() != AgentStatus.DISABLED)
                .orElse(false);
    }

    /**
     * 统计在线 Agent 数量
     */
    public long countActive() {
        return agentRepository.countByStatus(AgentStatus.ACTIVE);
    }

    /**
     * 安全获取或创建 Agent 配置
     * 处理 V2 迁移已插入配置导致唯一键冲突的情况
     */
    private AgentConfig getOrCreateConfig(Agent agent) {
        return agentConfigRepository.findByAgentId(agent.getId())
                .orElseGet(() -> {
                    try {
                        return createDefaultConfig(agent);
                    } catch (Exception e) {
                        // 唯一键冲突（配置已存在），重新查询
                        log.debug("Config already exists for agent_id={}, fetching", agent.getId());
                        return agentConfigRepository.findByAgentId(agent.getId())
                                .orElseThrow(() -> new RuntimeException("Failed to get or create config for agent: " + agent.getId()));
                    }
                });
    }

    /**
     * 创建默认配置
     */
    private AgentConfig createDefaultConfig(Agent agent) {
        AgentConfig config = AgentConfig.builder()
                .agent(agent)
                .logPaths("[]")
                .fimPaths("[]")
                .logInterval(5)
                .fimInterval(300)
                .inventoryInterval(3600)
                .heartbeatInterval(30)
                .build();
        return agentConfigRepository.save(config);
    }

    /**
     * Entity -> DTO（含 JSON 字段解析）
     */
    private AgentConfigDTO toConfigDTO(AgentConfig config) {
        return AgentConfigDTO.builder()
                .logPaths(parseJsonArray(config.getLogPaths()))
                .fimPaths(parseJsonArray(config.getFimPaths()))
                .logInterval(config.getLogInterval())
                .fimInterval(config.getFimInterval())
                .inventoryInterval(config.getInventoryInterval())
                .heartbeatInterval(config.getHeartbeatInterval())
                .build();
    }

    /**
     * 解析 JSON 数组字符串为 List<String>
     */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.debug("Failed to parse JSON array: {}", json);
            return Collections.emptyList();
        }
    }
}
