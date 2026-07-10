package com.secguard.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.secguard.common.enums.AlertStatus;
import com.secguard.common.enums.EventCategory;
import com.secguard.common.enums.Severity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 安全告警表
 */
@Entity
@Table(name = "sg_alert", indexes = {
        @Index(name = "idx_alert_agent", columnList = "agent_id"),
        @Index(name = "idx_alert_severity", columnList = "severity"),
        @Index(name = "idx_alert_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联 Agent */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    @JsonIgnore
    private Agent agent;

    /** Agent ID（序列化时使用） */
    @JsonProperty("agentId")
    public Long getAgentId() {
        return agent != null ? agent.getId() : null;
    }

    /** 触发规则 ID */
    private Long ruleId;

    /** 规则名称 */
    @Column(length = 256)
    private String ruleName;

    /** 严重等级 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    /** 事件类别 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventCategory category;

    /** 告警描述 */
    @Column(length = 1024)
    private String description;

    /** MITRE ATT&CK 战术 ID (JSON) */
    @Column(length = 256)
    private String mitreTactic;

    /** MITRE ATT&CK 技术 ID (JSON) */
    @Column(length = 256)
    private String mitreTechnique;

    /** 原始事件数据 (JSON) */
    @Column(columnDefinition = "JSON")
    private String rawEvent;

    /** 处置状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status;

    /** 来源 IP（从事件中解析） */
    @Column(length = 64)
    private String sourceIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = AlertStatus.OPEN;
    }
}
