package com.secguard.server.service;

import com.secguard.common.dto.*;
import com.secguard.common.enums.AgentStatus;
import com.secguard.server.entity.Agent;
import com.secguard.server.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Agent 管理服务
 *
 * W1 实现：注册、心跳、状态查询
 * W2 扩展：完整生命周期管理、配置下发
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private final AgentRepository agentRepository;

    @Value("${secguard.agent.heartbeat-timeout:120}")
    private int heartbeatTimeout;

    /**
     * Agent 注册
     * 生成唯一 agentKey，初始状态为 ACTIVE
     */
    @Transactional
    public AgentRegisterResponse register(AgentRegisterRequest request) {
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
        log.info("Agent registered: id={}, name={}, ip={}", agent.getId(), agent.getName(), agent.getIp());

        // 返回默认配置
        AgentConfigDTO defaultConfig = AgentConfigDTO.builder()
                .logInterval(5)
                .fimInterval(300)
                .inventoryInterval(3600)
                .heartbeatInterval(30)
                .build();

        return AgentRegisterResponse.builder()
                .agentId(agent.getId())
                .agentKey(agentKey)
                .config(defaultConfig)
                .build();
    }

    /**
     * Agent 心跳
     * 更新最后心跳时间和系统指标
     */
    @Transactional
    public void heartbeat(String agentKey, AgentHeartbeatRequest request) {
        Agent agent = agentRepository.findByAgentKey(agentKey)
                .orElseThrow(() -> new RuntimeException("Agent not found: " + agentKey));

        agent.setLastKeepalive(LocalDateTime.now());
        agent.setStatus(AgentStatus.ACTIVE);
        agent.setCpuUsage(request.getCpuUsage());
        agent.setMemUsage(request.getMemUsage());
        agent.setPid(request.getPid());
        agent.setUptime(request.getUptime());

        agentRepository.save(agent);
        log.debug("Agent heartbeat: key={}, cpu={}, mem={}", agentKey, request.getCpuUsage(), request.getMemUsage());
    }

    /**
     * 获取所有 Agent 列表
     */
    public List<Agent> listAgents() {
        return agentRepository.findAll();
    }

    /**
     * 根据 ID 获取 Agent 详情
     */
    public Agent getAgent(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent not found: " + id));
    }

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
}
