package com.secguard.server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 网络接口实体（对应 sg_asset_network 表）
 */
@Entity
@Table(name = "sg_asset_network")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetNetworkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "interface_name", nullable = false, length = 64)
    private String interfaceName;

    @Column(name = "mac_address", length = 32)
    private String macAddress;

    @Column(length = 64)
    private String ipv4;

    @Column(length = 128)
    private String ipv6;

    @Column(length = 64)
    private String gateway;

    @Column(length = 256)
    private String dns;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;
}
