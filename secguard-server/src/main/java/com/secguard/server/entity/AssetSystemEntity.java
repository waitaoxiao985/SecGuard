package com.secguard.server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 主机系统信息实体（对应 sg_asset_system 表）
 */
@Entity
@Table(name = "sg_asset_system")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSystemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(length = 128)
    private String hostname;

    @Column(length = 64)
    private String os;

    @Column(name = "os_version", length = 64)
    private String osVersion;

    @Column(length = 64)
    private String kernel;

    @Column(name = "cpu_model", length = 128)
    private String cpuModel;

    @Column(name = "cpu_cores")
    private Integer cpuCores;

    @Column(name = "ram_total_mb")
    private Long ramTotalMb;

    @Column(name = "uptime_seconds")
    private Long uptimeSeconds;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;
}
