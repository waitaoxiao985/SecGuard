package com.secguard.server.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 安全事件（规则匹配成功后产生）
 *
 * 包含触发规则信息、提取的字段、原始事件数据，
 * 用于生成告警记录并推送给前端。
 */
@Data
@Builder
public class SecurityEvent {

    /** 触发的规则编号 */
    private int ruleId;

    /** 规则名称 */
    private String ruleName;

    /** 规则严重等级 */
    private int ruleLevel;

    /** 事件类别 */
    private String category;

    /** 规则描述 */
    private String description;

    /** MITRE 战术 ID */
    private String mitreTactic;

    /** MITRE 技术 ID */
    private String mitreTechnique;

    /** 提取的字段（如 source_ip, username） */
    private Map<String, String> extractedFields;

    /** 原始事件字段 */
    private Map<String, String> eventFields;

    /** 原始日志内容 */
    private String rawLog;

    /** 事件来源文件 */
    private String source;

    /** Agent ID */
    private Long agentId;
}
