package com.secguard.server.engine;

import com.secguard.server.engine.model.FieldExtraction;
import com.secguard.server.engine.model.MatchCondition;
import com.secguard.server.engine.model.Rule;
import com.secguard.server.engine.model.SecurityEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则匹配器
 *
 * 将事件字段与规则条件进行匹配，支持四种操作符：
 * - equals:         精确字符串匹配
 * - contains:       子串包含
 * - regex:          正则表达式匹配
 * - numeric_range:  数值范围判断
 *
 * 匹配成功后执行字段提取（extract），生成 SecurityEvent。
 */
@Component
@Slf4j
public class RuleMatcher {

    // 缓存已编译的正则表达式（ConcurrentHashMap 保证多线程安全）
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

    /**
     * 尝试用一条规则匹配事件
     *
     * @param rule   检测规则
     * @param fields 事件的解析字段
     * @param rawLog 原始日志
     * @param source 日志来源
     * @param agentId Agent ID
     * @return 匹配成功返回 SecurityEvent，否则返回 null
     */
    public SecurityEvent matchRule(Rule rule, Map<String, String> fields,
                                    String rawLog, String source, Long agentId) {
        if (rule.getConditions() == null || rule.getConditions().getFieldMatch() == null) {
            return null;
        }

        // 所有条件必须匹配（AND 逻辑）
        for (MatchCondition condition : rule.getConditions().getFieldMatch()) {
            if (!matchCondition(condition, fields)) {
                return null;
            }
        }

        // 全部条件通过 → 执行字段提取
        Map<String, String> extracted = extractFields(rule, fields);

        // 构建 MITRE 标签字符串
        String mitreTactic = null;
        String mitreTechnique = null;
        if (rule.getMitre() != null) {
            if (rule.getMitre().getTactic() != null) {
                mitreTactic = String.join(",", rule.getMitre().getTactic());
            }
            if (rule.getMitre().getTechnique() != null) {
                mitreTechnique = String.join(",", rule.getMitre().getTechnique());
            }
        }

        return SecurityEvent.builder()
                .ruleId(rule.getRuleId())
                .ruleName(rule.getName())
                .ruleLevel(rule.getLevel())
                .category(rule.getCategory())
                .description(rule.getDescription())
                .mitreTactic(mitreTactic)
                .mitreTechnique(mitreTechnique)
                .extractedFields(extracted)
                .eventFields(fields)
                .rawLog(rawLog)
                .source(source)
                .agentId(agentId)
                .build();
    }

    /**
     * 匹配单个条件
     */
    private boolean matchCondition(MatchCondition condition, Map<String, String> fields) {
        String fieldValue = getFieldValue(fields, condition.getField());
        if (fieldValue == null) return false;

        String operator = condition.getOperator();
        if (operator == null) return false;

        return switch (operator.toLowerCase()) {
            case "equals" -> fieldValue.equals(condition.getValue());
            case "contains" -> fieldValue.toLowerCase().contains(
                    condition.getValue() != null ? condition.getValue().toLowerCase() : "");
            case "regex" -> matchRegex(fieldValue, condition.getValue());
            case "numeric_range" -> matchNumericRange(fieldValue, condition.getMin(), condition.getMax());
            default -> {
                log.warn("Unknown operator: {}", operator);
                yield false;
            }
        };
    }

    /**
     * 正则匹配（带缓存）
     */
    private boolean matchRegex(String fieldValue, String regex) {
        if (regex == null) return false;
        try {
            Pattern pattern = patternCache.computeIfAbsent(regex, Pattern::compile);
            return pattern.matcher(fieldValue).find();
        } catch (Exception e) {
            log.warn("Invalid regex pattern: {}", regex, e);
            return false;
        }
    }

    /**
     * 数值范围匹配
     */
    private boolean matchNumericRange(String fieldValue, Double min, Double max) {
        try {
            double value = Double.parseDouble(fieldValue);
            boolean aboveMin = min == null || value >= min;
            boolean belowMax = max == null || value <= max;
            return aboveMin && belowMax;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 执行字段提取
     */
    private Map<String, String> extractFields(Rule rule, Map<String, String> fields) {
        Map<String, String> extracted = new LinkedHashMap<>();

        if (rule.getConditions().getExtract() == null) {
            return extracted;
        }

        for (FieldExtraction extraction : rule.getConditions().getExtract()) {
            String sourceField = getFieldValue(fields, extraction.getFrom());
            if (sourceField == null) continue;

            try {
                Pattern pattern = patternCache.computeIfAbsent(extraction.getRegex(), Pattern::compile);
                Matcher matcher = pattern.matcher(sourceField);
                if (matcher.find() && matcher.groupCount() > 0) {
                    extracted.put(extraction.getField(), matcher.group(1));
                }
            } catch (Exception e) {
                log.warn("Field extraction failed for {}: {}", extraction.getField(), e.getMessage());
            }
        }

        return extracted;
    }

    /**
     * 获取字段值（支持常见别名）
     */
    private String getFieldValue(Map<String, String> fields, String fieldName) {
        if (fields == null || fieldName == null) return null;
        String value = fields.get(fieldName);
        if (value != null) return value;
        // 尝试常见的别名映射
        return switch (fieldName) {
            case "source_ip", "src_ip", "ip" -> fields.getOrDefault("source_ip",
                    fields.getOrDefault("src_ip", fields.get("ip")));
            default -> null;
        };
    }
}
