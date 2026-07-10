package com.secguard.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 主机系统信息（Agent -> Server）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSystemDTO {

    /** 主机名 */
    private String hostname;

    /** 操作系统 (windows/linux/macos) */
    private String os;

    /** 操作系统版本 */
    private String osVersion;

    /** 内核版本 */
    private String kernel;

    /** CPU 型号 */
    private String cpuModel;

    /** CPU 核心数 */
    private Integer cpuCores;

    /** 物理内存总量 (MB) */
    private Long ramTotalMb;

    /** 系统运行时间 (秒) */
    private Long uptimeSeconds;

    /** 采集时间 */
    private Instant collectedAt;
}
