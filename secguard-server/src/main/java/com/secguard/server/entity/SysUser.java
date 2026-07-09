package com.secguard.server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统用户表
 */
@Entity
@Table(name = "sg_sys_user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名 */
    @Column(nullable = false, unique = true, length = 64)
    private String username;

    /** 密码哈希 (BCrypt) */
    @Column(nullable = false, length = 256)
    private String passwordHash;

    /** 角色: admin/operator/viewer */
    @Column(nullable = false, length = 32)
    private String role;

    /** 显示名称 */
    @Column(length = 128)
    private String displayName;

    /** 邮箱 */
    @Column(length = 128)
    private String email;

    /** 是否启用 */
    @Column(nullable = false)
    private Boolean enabled;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (enabled == null) enabled = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
