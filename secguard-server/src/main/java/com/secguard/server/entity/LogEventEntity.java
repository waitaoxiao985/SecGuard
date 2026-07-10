package com.secguard.server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 日志事件实体（对应 sg_log_event 表）
 *
 * 存储 Agent 上报的原始日志事件，供后续规则引擎消费生成告警。
 */
@Entity
@Table(name = "sg_log_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    /** 日志来源文件路径 */
    @Column(name = "source", length = 1024)
    private String source;

    /** 日志格式: syslog/json/plain */
    @Column(length = 32)
    private String format;

    /** 原始日志内容 */
    @Column(name = "raw_log", nullable = false, columnDefinition = "TEXT")
    private String rawLog;

    /** 解析后的结构化字段（JSON） */
    @Column(columnDefinition = "JSON")
    private String fields;

    /** 预分类事件类别 */
    @Column(length = 32)
    private String category;

    /** 事件时间戳 */
    @Column(name = "event_time")
    private LocalDateTime eventTime;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
