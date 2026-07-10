package com.secguard.server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 配置下发表
 */
@Entity
@Table(name = "sg_agent_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联 Agent（1:1） */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false, unique = true)
    private Agent agent;

    /** 日志采集路径（JSON） */
    @Column(columnDefinition = "JSON")
    private String logPaths;

    /** FIM 监控路径（JSON） */
    @Column(columnDefinition = "JSON")
    private String fimPaths;

    /** 日志采集间隔（秒） */
    @Builder.Default
    private Integer logInterval = 5;

    /** FIM 扫描间隔（秒） */
    @Builder.Default
    private Integer fimInterval = 300;

    /** 资产采集间隔（秒） */
    @Builder.Default
    private Integer inventoryInterval = 3600;

    /** 心跳间隔（秒） */
    @Builder.Default
    private Integer heartbeatInterval = 30;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
