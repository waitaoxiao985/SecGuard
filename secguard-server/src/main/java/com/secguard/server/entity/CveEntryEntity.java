package com.secguard.server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CVE 漏洞库实体（对应 sg_cve_entry 表）
 *
 * 存储已知漏洞信息，用于与 Agent 软件清单进行匹配。
 */
@Entity
@Table(name = "sg_cve_entry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CveEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** CVE 编号 (如 CVE-2024-12345) */
    @Column(name = "cve_id", nullable = false, length = 32)
    private String cveId;

    /** 严重等级 (LOW/MEDIUM/HIGH/CRITICAL) */
    @Column(nullable = false, length = 20)
    private String severity;

    /** CVSS 评分 (0.0-10.0) */
    @Column(name = "cvss_score", precision = 3, scale = 1)
    private BigDecimal cvssScore;

    /** 漏洞描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 受影响产品名称 */
    @Column(name = "affected_product", length = 256)
    private String affectedProduct;

    /** 受影响版本 (格式如 "< 2.0.0" 或 "= 1.5.3") */
    @Column(name = "affected_version", length = 256)
    private String affectedVersion;

    /** 发布日期 */
    @Column(name = "published_at")
    private LocalDate publishedAt;
}
