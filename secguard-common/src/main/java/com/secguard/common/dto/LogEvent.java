package com.secguard.common.dto;

import com.secguard.common.enums.EventCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 日志事件（Agent -> Server）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEvent {

    /** 事件时间戳 */
    private Instant timestamp;

    /** 原始日志内容 */
    private String rawLog;

    /** 日志来源文件 */
    private String source;

    /** 日志格式类型: syslog/json/plain */
    private String format;

    /** 解析后的结构化字段 */
    private Map<String, String> fields;

    /** 事件类别（Agent 预分类） */
    private EventCategory category;
}
