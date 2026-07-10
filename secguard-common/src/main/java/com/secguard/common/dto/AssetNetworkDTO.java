package com.secguard.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网络接口条目（Agent -> Server）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetNetworkDTO {

    /** 接口名称 (如 eth0, Ethernet0) */
    private String interfaceName;

    /** MAC 地址 */
    private String macAddress;

    /** IPv4 地址 */
    private String ipv4;

    /** IPv6 地址 */
    private String ipv6;

    /** 默认网关 */
    private String gateway;

    /** DNS 服务器 */
    private String dns;
}
