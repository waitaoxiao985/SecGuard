package com.secguard.server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 已安装软件实体（对应 sg_asset_software 表）
 */
@Entity
@Table(name = "sg_asset_software")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSoftwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(length = 64)
    private String version;

    @Column(length = 128)
    private String vendor;

    @Column(length = 32)
    private String format;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;
}
