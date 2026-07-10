package com.secguard.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 配置属性（映射 application.yml 中 secguard.* 前缀）
 */
@Component
@ConfigurationProperties(prefix = "secguard")
@Data
public class AgentProperties {

    private AgentConfig agent = new AgentConfig();
    private LogConfig log = new LogConfig();

    @Data
    public static class AgentConfig {
        /** Server 地址 */
        private String serverUrl = "http://localhost:8900";
        /** Agent 名称 */
        private String name = "secguard-agent";
        /** 本地数据目录（存放 agent_key、offset 等） */
        private String dataDir = ".secguard";
    }

    @Data
    public static class LogConfig {
        /** 日志采集路径列表 */
        private List<String> paths = new ArrayList<>();
        /** 采集间隔（秒） */
        private int interval = 5;
        /** 单次最大采集条数 */
        private int maxBatchSize = 100;
    }
}
