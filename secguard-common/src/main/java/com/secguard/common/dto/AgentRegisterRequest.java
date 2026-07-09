package com.secguard.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 注册请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRegisterRequest {

    @NotBlank(message = "Agent名称不能为空")
    private String name;

    @NotBlank(message = "Agent IP不能为空")
    private String ip;

    /** 操作系统类型: windows/linux/macos */
    @NotBlank(message = "操作系统不能为空")
    private String os;

    /** 操作系统版本 */
    private String osVersion;

    /** Agent 版本号 */
    private String agentVersion;

    /** 主机名 */
    private String hostname;
}
