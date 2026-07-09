package com.secguard.server.entity;

import com.secguard.common.enums.AgentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 注册表
 */
@Entity
@Table(name = "sg_agent", indexes = {
        @Index(name = "idx_agent_key", columnList = "agentKey", unique = true),
        @Index(name = "idx_agent_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Agent 唯一密钥（UUID） */
    @Column(nullable = false, unique = true, length = 64)
    private String agentKey;

    /** Agent 名称 */
    @Column(nullable = false, length = 128)
    private String name;

    /** Agent IP 地址 */
    @Column(nullable = false, length = 64)
    private String ip;

    /** 操作系统: windows/linux/macos */
    @Column(nullable = false, length = 32)
    private String os;

    /** 操作系统版本 */
    @Column(length = 64)
    private String osVersion;

    /** 主机名 */
    @Column(length = 128)
    private String hostname;

    /** Agent 版本号 */
    @Column(length = 32)
    private String agentVersion;

    /** 状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentStatus status;

    /** 最后心跳时间 */
    private LocalDateTime lastKeepalive;

    /** 系统负载 */
    private Double cpuUsage;

    /** 内存使用率 */
    private Double memUsage;

    /** Agent 进程 ID */
    private Integer pid;

    /** 运行时长（秒） */
    private Long uptime;

    /** 注册时间 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
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
