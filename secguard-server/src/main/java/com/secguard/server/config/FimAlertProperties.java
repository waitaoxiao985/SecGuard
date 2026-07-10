package com.secguard.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * FIM 高危文件监控规则配置
 *
 * 从 application.yml 中读取 secguard.fim 前缀的配置，
 * 定义需要触发告警的高危文件路径模式。
 */
@Component
@ConfigurationProperties(prefix = "secguard.fim")
@Data
public class FimAlertProperties {

    private List<HighRiskPath> highRiskPaths = new ArrayList<>();

    @Data
    public static class HighRiskPath {
        /** 路径匹配模式（contains 匹配） */
        private String pattern;
        /** 告警严重等级（Wazuh rule level 0-15） */
        private int severity = 10;
        /** 告警描述 */
        private String description;
        /** MITRE ATT&CK 技术 ID */
        private String mitreTechnique;
        /** MITRE ATT&CK 战术 ID */
        private String mitreTactic;
    }
}
