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
import java.lang.management.OperatingSystemMXBean;
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

    @Scheduled(fixedDelayString = "${secguard.log.interval:5}000", initialDelay = 5000)
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
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double load = osBean.getSystemLoadAverage();
            int cpus = osBean.getAvailableProcessors();
            return cpus > 0 ? Math.min(100.0, (load / cpus) * 100) : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Double getMemUsage() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long max = runtime.maxMemory();
        long used = total - free;
        return (double) used / max * 100;
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
