package com.secguard.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Agent 心跳请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentHeartbeatRequest {

    /** Agent 当前运行状态摘要 */
    private String status;

    /** 系统负载 */
    private Double cpuUsage;

    /** 内存使用率 */
    private Double memUsage;

    /** Agent 进程 ID */
    private Integer pid;

    /** 运行时长（秒） */
    private Long uptime;
}
