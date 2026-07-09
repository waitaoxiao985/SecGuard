package com.secguard.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agent 配置 DTO（Server下发给Agent）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfigDTO {

    /** 日志采集路径列表 */
    private List<String> logPaths;

    /** FIM 监控路径列表 */
    private List<String> fimPaths;

    /** 日志采集间隔（秒） */
    private Integer logInterval;

    /** FIM 扫描间隔（秒） */
    private Integer fimInterval;

    /** 资产采集间隔（秒） */
    private Integer inventoryInterval;

    /** 心跳间隔（秒） */
    private Integer heartbeatInterval;
}
