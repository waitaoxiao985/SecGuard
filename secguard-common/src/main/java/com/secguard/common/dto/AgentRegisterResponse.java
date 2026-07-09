package com.secguard.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 注册响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRegisterResponse {

    /** 服务端分配的 Agent ID */
    private Long agentId;

    /** Agent 唯一密钥，后续通信需携带 */
    private String agentKey;

    /** 服务端下发的初始配置 */
    private AgentConfigDTO config;
}
