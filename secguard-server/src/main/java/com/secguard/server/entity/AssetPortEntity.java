package com.secguard.server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 开放端口实体（对应 sg_asset_port 表）
 */
@Entity
@Table(name = "sg_asset_port")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetPortEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(nullable = false, length = 16)
    private String protocol;

    @Column(name = "local_port", nullable = false)
    private Integer localPort;

    @Column(length = 32)
    private String state;

    @Column(name = "process_name", length = 128)
    private String processName;

    @Column(name = "process_pid")
    private Integer processPid;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;
}
