package com.secguard.server.dto;

import com.secguard.common.dto.AgentConfigDTO;
import com.secguard.server.entity.Agent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 详情响应（含配置信息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDetailResponse {

    /** Agent 基础信息 */
    private Agent agent;

    /** Agent 配置 */
    private AgentConfigDTO config;
}
