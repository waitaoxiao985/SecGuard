package com.secguard.agent;

import com.secguard.agent.registration.AgentRegistrar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Agent 启动初始化器
 *
 * 在 Spring Boot 应用完全就绪后自动执行 Agent 注册。
 * 注册成功后，心跳和日志采集的 @Scheduled 任务会自动开始工作。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentStartupRunner {

    private final AgentRegistrar registrar;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            log.info("SecGuard Agent starting up...");
            String agentKey = registrar.register();
            log.info("Agent registered successfully, key={}", agentKey);
        } catch (Exception e) {
            log.error("Agent registration failed: {}", e.getMessage(), e);
            log.warn("Agent will retry on next heartbeat cycle");
        }
    }
}
