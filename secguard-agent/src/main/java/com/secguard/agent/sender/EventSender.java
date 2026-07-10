package com.secguard.agent.sender;

import com.secguard.agent.config.AgentProperties;
import com.secguard.common.dto.ApiResponse;
import com.secguard.common.dto.FIMBaselineSnapshot;
import com.secguard.common.dto.FIMEvent;
import com.secguard.common.dto.LogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 事件批量发送器
 *
 * 将采集到的日志事件批量 POST 到 Server，支持重试（最多 3 次，指数退避）。
 * 请求头携带 X-Agent-Key 进行身份认证。
 */
@Component
@Slf4j
public class EventSender {

    private final RestTemplate restTemplate;
    private final AgentProperties properties;

    public EventSender(AgentProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 批量发送日志事件到 Server
     *
     * @param events  日志事件列表
     * @param agentKey Agent 密钥
     * @return 是否发送成功
     */
    public boolean sendLogEvents(List<LogEvent> events, String agentKey) {
        if (events == null || events.isEmpty()) return true;

        String url = properties.getAgent().getServerUrl() + "/api/events/logs";
        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Agent-Key", agentKey);

                HttpEntity<List<LogEvent>> entity = new HttpEntity<>(events, headers);
                ResponseEntity<ApiResponse> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, ApiResponse.class);

                if (response.getStatusCode().is2xxSuccessful()
                        && response.getBody() != null
                        && response.getBody().getCode() == 200) {
                    log.info("Sent {} log events to server", events.size());
                    return true;
                }

                log.warn("Server responded with error: {}", response.getBody());
            } catch (Exception e) {
                log.warn("Failed to send events (attempt {}/{}): {}",
                        attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        log.error("Failed to send {} events after {} attempts", events.size(), maxRetries);
        return false;
    }

    /**
     * 批量发送 FIM 事件到 Server
     *
     * @param events   FIM 事件列表
     * @param agentKey Agent 密钥
     * @return 是否发送成功
     */
    public boolean sendFimEvents(List<FIMEvent> events, String agentKey) {
        if (events == null || events.isEmpty()) return true;

        String url = properties.getAgent().getServerUrl() + "/api/events/fim";
        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Agent-Key", agentKey);

                HttpEntity<List<FIMEvent>> entity = new HttpEntity<>(events, headers);
                ResponseEntity<ApiResponse> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, ApiResponse.class);

                if (response.getStatusCode().is2xxSuccessful()
                        && response.getBody() != null
                        && response.getBody().getCode() == 200) {
                    log.info("Sent {} FIM events to server", events.size());
                    return true;
                }

                log.warn("Server responded with error for FIM events: {}", response.getBody());
            } catch (Exception e) {
                log.warn("Failed to send FIM events (attempt {}/{}): {}",
                        attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        log.error("Failed to send {} FIM events after {} attempts", events.size(), maxRetries);
        return false;
    }

    /**
     * 发送 FIM 基线快照到 Server
     *
     * @param snapshot 基线快照
     * @param agentKey Agent 密钥
     * @return 是否发送成功
     */
    public boolean sendBaselineSnapshot(FIMBaselineSnapshot snapshot, String agentKey) {
        if (snapshot == null || snapshot.getEntries() == null || snapshot.getEntries().isEmpty()) {
            return true;
        }

        String url = properties.getAgent().getServerUrl() + "/api/events/fim/baseline";
        int maxRetries = 2; // 基线上报重试次数较少（非关键路径）

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Agent-Key", agentKey);

                HttpEntity<FIMBaselineSnapshot> entity = new HttpEntity<>(snapshot, headers);
                ResponseEntity<ApiResponse> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, ApiResponse.class);

                if (response.getStatusCode().is2xxSuccessful()
                        && response.getBody() != null
                        && response.getBody().getCode() == 200) {
                    log.info("Sent FIM baseline snapshot ({} entries, version={}) to server",
                            snapshot.getEntries().size(), snapshot.getVersion());
                    return true;
                }

                log.warn("Server responded with error for baseline: {}", response.getBody());
            } catch (Exception e) {
                log.warn("Failed to send baseline (attempt {}/{}): {}",
                        attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        log.warn("Failed to send baseline snapshot after {} attempts", maxRetries);
        return false;
    }
}
