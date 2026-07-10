package com.secguard.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 心跳响应（Server -> Agent）
 * 携带最新配置，Agent 对比后决定是否更新
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentHeartbeatResponse {

    /** 最新配置（null 表示无变更） */
    private AgentConfigDTO config;
}
