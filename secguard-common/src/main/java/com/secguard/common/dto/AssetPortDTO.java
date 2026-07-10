package com.secguard.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 开放端口条目（Agent -> Server）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetPortDTO {

    /** 协议 (tcp/udp) */
    private String protocol;

    /** 本地端口号 */
    private Integer localPort;

    /** 状态 (LISTEN/ESTABLISHED 等) */
    private String state;

    /** 进程名称 */
    private String processName;

    /** 进程 PID */
    private Integer processPid;
}
