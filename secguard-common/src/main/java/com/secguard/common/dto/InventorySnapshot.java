package com.secguard.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 主机资产快照（Agent 一次性上报全部资产信息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySnapshot {

    /** 系统信息 */
    private AssetSystemDTO system;

    /** 已安装软件列表 */
    private List<AssetSoftwareDTO> software;

    /** 开放端口列表 */
    private List<AssetPortDTO> ports;

    /** 网络接口列表 */
    private List<AssetNetworkDTO> networks;

    /** 采集时间 */
    private Instant collectedAt;
}
