package com.secguard.agent.heartbeat;

import com.secguard.agent.config.AgentProperties;
import com.secguard.agent.registration.AgentRegistrar;
import com.secguard.common.dto.AgentHeartbeatRequest;
import com.secguard.common.dto.AgentHeartbeatResponse;
import com.secguard.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * 心跳任务
 *
 * 定期向 Server 发送心跳，报告系统指标（CPU/内存/PID/运行时长），
 * 并接收 Server 下发的最新配置。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HeartbeatTask {

    private final AgentProperties properties;
    private final AgentRegistrar registrar;

    private RestTemplate restTemplate;
    private long startTime = System.currentTimeMillis();

    @Scheduled(fixedDelayString = "${secguard.heartbeat.interval:30}000", initialDelay = 5000)
    public void heartbeat() {
        if (registrar.getAgentKey() == null) return;

        if (restTemplate == null) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(10000);
            restTemplate = new RestTemplate(factory);
        }

        try {
            AgentHeartbeatRequest request = AgentHeartbeatRequest.builder()
                    .status("RUNNING")
                    .cpuUsage(getCpuUsage())
                    .memUsage(getMemUsage())
                    .pid(getPid())
                    .uptime((System.currentTimeMillis() - startTime) / 1000)
                    .build();

            String url = properties.getAgent().getServerUrl() + "/api/agents/heartbeat";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Agent-Key", registrar.getAgentKey());

            HttpEntity<AgentHeartbeatRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<ApiResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, ApiResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Heartbeat sent: cpu={}, mem={}%",
                        String.format("%.1f", request.getCpuUsage()),
                        String.format("%.1f", request.getMemUsage()));
            }
        } catch (Exception e) {
            log.warn("Heartbeat failed: {}", e.getMessage());
        }
    }

    private Double getCpuUsage() {
        try {
            // 使用 com.sun.management 获取系统级 CPU 使用率
            com.sun.management.OperatingSystemMXBean sunBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = sunBean.getCpuLoad(); // 0.0 ~ 1.0
            if (cpuLoad < 0) {
                // getCpuLoad() 在首次调用或不可用时返回 -1，回退到 getSystemLoadAverage
                double loadAvg = sunBean.getSystemLoadAverage();
                if (loadAvg < 0) return 0.0; // Windows 下不可用
                int cpus = sunBean.getAvailableProcessors();
                return cpus > 0 ? Math.min(100.0, Math.max(0.0, (loadAvg / cpus) * 100)) : 0.0;
            }
            return Math.round(cpuLoad * 10000.0) / 100.0; // 保留两位小数
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Double getMemUsage() {
        try {
            // 使用 com.sun.management 获取系统级内存使用率
            com.sun.management.OperatingSystemMXBean sunBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long totalMemory = sunBean.getTotalMemorySize();
            long freeMemory = sunBean.getFreeMemorySize();
            if (totalMemory <= 0) return 0.0;
            long usedMemory = totalMemory - freeMemory;
            return Math.round((double) usedMemory / totalMemory * 10000.0) / 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Integer getPid() {
        try {
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            return (int) runtimeMXBean.getPid();
        } catch (Exception e) {
            return -1;
        }
    }
}
