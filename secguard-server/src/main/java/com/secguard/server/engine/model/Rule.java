package com.secguard.server.engine.model;

import lombok.Data;

import java.util.List;

/**
 * 检测规则（YAML 加载后的内部表示）
 *
 * 对标 Wazuh 规则结构，支持：
 * - 多条件 AND 匹配（field_match 列表）
 * - 匹配后字段提取（extract 列表）
 * - MITRE ATT&CK 战术/技术映射
 * - PCI DSS 合规映射
 */
@Data
public class Rule {

    /** 规则编号（Wazuh 风格） */
    private int ruleId;

    /** 规则名称 */
    private String name;

    /** 严重等级 (0-15) */
    private int level;

    /** 事件类别 */
    private String category;

    /** 规则描述 */
    private String description;

    /** 匹配条件列表（AND 关系） */
    private Conditions conditions;

    /** MITRE ATT&CK 映射 */
    private MitreMapping mitre;

    /** PCI DSS 合规条款 */
    private List<String> pciDss;

    /**
     * 条件块
     */
    @Data
    public static class Conditions {
        private List<MatchCondition> fieldMatch;
        private List<FieldExtraction> extract;
    }

    /**
     * MITRE ATT&CK 映射
     */
    @Data
    public static class MitreMapping {
        private List<String> tactic;
        private List<String> technique;
    }
}
