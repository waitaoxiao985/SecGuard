package com.secguard.agent.registration;

import com.secguard.agent.config.AgentProperties;
import com.secguard.common.dto.AgentRegisterRequest;
import com.secguard.common.dto.AgentRegisterResponse;
import com.secguard.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Agent 注册器
 *
 * 启动时检查本地是否已有 agent_key：
 * - 有：直接复用（服务端去重机制会返回已有 key）
 * - 无：调用注册 API 获取新 key，持久化到本地文件
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentRegistrar {

    private final AgentProperties properties;

    private String agentKey;
    private Long agentId;
    private RestTemplate restTemplate;

    /**
     * 注册或恢复 Agent 身份
     *
     * @return Agent 密钥
     */
    public String register() throws IOException {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        restTemplate = new RestTemplate(factory);

        // 检查本地是否已保存 key
        Path dataDir = Paths.get(properties.getAgent().getDataDir());
        Files.createDirectories(dataDir);
        Path keyFile = dataDir.resolve("agent.key");
        Path idFile = dataDir.resolve("agent.id");

        if (Files.exists(keyFile)) {
            agentKey = Files.readString(keyFile).trim();
            agentId = Files.exists(idFile) ? Long.parseLong(Files.readString(idFile).trim()) : null;
            log.info("Loaded existing agent key: {}", agentKey);
        }

        // 调用注册 API
        String hostname = InetAddress.getLocalHost().getHostName();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String os = System.getProperty("os.name", "unknown").toLowerCase();

        AgentRegisterRequest request = AgentRegisterRequest.builder()
                .name(properties.getAgent().getName())
                .ip(ip)
                .os(os)
                .osVersion(System.getProperty("os.version"))
                .agentVersion("1.0.0")
                .hostname(hostname)
                .build();

        String url = properties.getAgent().getServerUrl() + "/api/agents/register";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AgentRegisterRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, ApiResponse.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            // ApiResponse.data 是一个 Map，从中提取 AgentRegisterResponse 字段
            Object data = response.getBody().getData();
            if (data instanceof java.util.Map<?, ?> map) {
                Object keyObj = map.get("agentKey");
                Object idObj = map.get("agentId");

                if (keyObj == null) {
                    throw new IOException("Server response missing agentKey");
                }

                agentKey = (String) keyObj;
                agentId = idObj instanceof Number ? ((Number) idObj).longValue() : null;

                // 持久化到本地
                Files.writeString(keyFile, agentKey);
                if (agentId != null) {
                    Files.writeString(idFile, String.valueOf(agentId));
                }

                log.info("Agent registered: id={}, key={}, hostname={}, ip={}",
                        agentId, agentKey, hostname, ip);
            }
        } else {
            throw new IOException("Failed to register agent: " + response.getBody());
        }

        return agentKey;
    }

    public String getAgentKey() {
        return agentKey;
    }

    public Long getAgentId() {
        return agentId;
    }
}
