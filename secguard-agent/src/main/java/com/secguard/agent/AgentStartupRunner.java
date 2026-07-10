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
 * 注册失败时自动重试（最多 3 次，指数退避），
 * 注册成功后，心跳和日志采集的 @Scheduled 任务会自动开始工作。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentStartupRunner {

    private final AgentRegistrar registrar;

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 5000;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("SecGuard Agent starting up...");

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String agentKey = registrar.register();
                log.info("Agent registered successfully, key={}", agentKey);
                return;
            } catch (Exception e) {
                log.error("Agent registration attempt {}/{} failed: {}",
                        attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    long delay = BASE_DELAY_MS * (1L << (attempt - 1));
                    log.info("Retrying in {}ms...", delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Registration retry interrupted");
                        return;
                    }
                }
            }
        }

        log.error("Agent registration failed after {} attempts. "
                + "Scheduled tasks will remain dormant until restart.", MAX_RETRIES);
    }
}
