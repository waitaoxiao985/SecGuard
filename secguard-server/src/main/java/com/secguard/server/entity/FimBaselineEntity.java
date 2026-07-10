package com.secguard.server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FIM 文件基线实体（对应 sg_fim_baseline 表）
 *
 * 存储 Server 端接收到的 Agent 文件基线快照，
 * 支持基线查询、对比和重置。
 */
@Entity
@Table(name = "sg_fim_baseline")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FimBaselineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(length = 32)
    private String permissions;

    @Column(length = 128)
    private String owner;

    @Column(name = "baseline_version", nullable = false)
    @Builder.Default
    private Integer baselineVersion = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
