package com.secguard.server.scheduler;

import com.secguard.server.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agent 健康检查定时任务
 *
 * 每 30 秒扫描一次，将心跳超时的 Agent 标记为 DISCONNECTED。
 * 超时时间由 secguard.agent.heartbeat-timeout 配置（默认 120 秒）。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentHealthChecker {

    private final AgentService agentService;

    @Scheduled(fixedDelayString = "${secguard.agent.health-check-interval:30000}")
    public void checkAgentHealth() {
        try {
            int count = agentService.markDisconnectedAgents();
            if (count > 0) {
                log.info("Health check: {} agent(s) marked as DISCONNECTED", count);
            }
        } catch (Exception e) {
            log.error("Health check failed", e);
        }
    }
}
