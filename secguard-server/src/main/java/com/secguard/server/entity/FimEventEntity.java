package com.secguard.server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FIM 变更事件实体（对应 sg_fim_event 表）
 *
 * 存储 Agent 上报的文件完整性变更事件（新增/修改/删除）。
 */
@Entity
@Table(name = "sg_fim_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FimEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    /** 变更文件路径 */
    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;

    /** 变更类型: ADDED / MODIFIED / DELETED */
    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;

    /** 当前 SHA-256 哈希 */
    @Column(name = "sha256_hash", length = 64)
    private String sha256Hash;

    /** 变更前哈希 */
    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    /** 文件大小（字节） */
    @Column(name = "file_size")
    private Long fileSize;

    /** 文件权限 */
    @Column(length = 32)
    private String permissions;

    /** 文件属主 */
    @Column(length = 128)
    private String owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
