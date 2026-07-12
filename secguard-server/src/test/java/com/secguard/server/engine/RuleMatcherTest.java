package com.secguard.server.engine;

import com.secguard.server.engine.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RuleMatcher 单元测试
 *
 * 覆盖四种操作符（equals/contains/regex/numeric_range）+ 字段提取 + MITRE 映射
 */
class RuleMatcherTest {

    private RuleMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new RuleMatcher();
    }

    // ===== 辅助方法 =====

    private Rule buildRule(MatchCondition... conditions) {
        Rule rule = new Rule();
        rule.setRuleId(1001);
        rule.setName("Test Rule");
        rule.setLevel(8);
        rule.setCategory("authentication");
        rule.setDescription("Test description");

        Rule.Conditions conds = new Rule.Conditions();
        conds.setFieldMatch(List.of(conditions));
        rule.setConditions(conds);
        return rule;
    }

    private MatchCondition cond(String field, String operator, String value) {
        MatchCondition c = new MatchCondition();
        c.setField(field);
        c.setOperator(operator);
        c.setValue(value);
        return c;
    }

    private MatchCondition condRange(String field, Double min, Double max) {
        MatchCondition c = new MatchCondition();
        c.setField(field);
        c.setOperator("numeric_range");
        c.setMin(min);
        c.setMax(max);
        return c;
    }

    private Map<String, String> fields(Map.Entry<String, String>... entries) {
        Map<String, String> map = new HashMap<>();
        for (var e : entries) map.put(e.getKey(), e.getValue());
        return map;
    }

    // ===== equals 操作符 =====

    @Nested
    @DisplayName("equals operator")
    class EqualsTests {

        @Test
        @DisplayName("精确匹配成功")
        void matchExact() {
            Rule rule = buildRule(cond("program", "equals", "sshd"));
            Map<String, String> f = fields(Map.entry("program", "sshd"));

            SecurityEvent event = matcher.matchRule(rule, f, "raw log", "/var/log/auth.log", 1L);
            assertNotNull(event);
            assertEquals("Test Rule", event.getRuleName());
        }

        @Test
        @DisplayName("精确匹配失败 — 值不同")
        void noMatchDifferentValue() {
            Rule rule = buildRule(cond("program", "equals", "sshd"));
            Map<String, String> f = fields(Map.entry("program", "nginx"));

            assertNull(matcher.matchRule(rule, f, "raw", "src", 1L));
        }

        @Test
        @DisplayName("字段不存在 — 匹配失败")
        void noMatchMissingField() {
            Rule rule = buildRule(cond("program", "equals", "sshd"));
            Map<String, String> f = fields(Map.entry("other", "value"));

            assertNull(matcher.matchRule(rule, f, "raw", "src", 1L));
        }
    }

    // ===== contains 操作符 =====

    @Nested
    @DisplayName("contains operator")
    class ContainsTests {

        @Test
        @DisplayName("子串包含匹配")
        void matchContains() {
            Rule rule = buildRule(cond("message", "contains", "Failed password"));
            Map<String, String> f = fields(Map.entry("message", "Failed password for root from 192.168.1.1"));

            assertNotNull(matcher.matchRule(rule, f, "raw", "src", 1L));
        }

        @Test
        @DisplayName("大小写不敏感")
        void matchCaseInsensitive() {
            Rule rule = buildRule(cond("message", "contains", "failed"));
            Map<String, String> f = fields(Map.entry("message", "FAILED login attempt"));

            assertNotNull(matcher.matchRule(rule, f, "raw", "src", 1L));
        }

        @Test
        @DisplayName("不包含 — 匹配失败")
        void noMatchContains() {
            Rule rule = buildRule(cond("message", "contains", "Failed password"));
            Map<String, String> f = fields(Map.entry("message", "Accepted publickey for user"));

            assertNull(matcher.matchRule(rule, f, "raw", "src", 1L));
        }
    }

    // ===== regex 操作符 =====

    @Nested
    @DisplayName("regex operator")
    class RegexTests {

        @Test
        @DisplayName("正则匹配成功")
        void matchRegex() {
            Rule rule = buildRule(cond("message", "regex", "Failed password for .* from (\\d+\\.\\d+\\.\\d+\\.\\d+)"));
            Map<String, String> f = fields(Map.entry("message", "Failed password for root from 192.168.1.100 port 22"));

            assertNotNull(matcher.matchRule(rule, f, "raw", "src", 1L));
        }

        @Test
        @DisplayName("正则不匹配 — 返回 null")
        void noMatchRegex() {
            Rule rule = buildRule(cond("message", "regex", "^ERROR\\s+\\d+"));
            Map<String, String> f = fields(Map.entry("message", "INFO service started"));

            assertNull(matcher.matchRule(rule, f, "raw", "src", 1L));
        }

        @Test
        @DisplayName("无效正则 — 不抛异常，返回 false")
        void invalidRegexNoException() {
            Rule rule = buildRule(cond("message", "regex", "[invalid"));
            Map<String, String> f = fields(Map.entry("message", "test"));

            assertNull(matcher.matchRule(rule, f, "raw", "src", 1L));
        }
    }

    // ===== numeric_range 操作符 =====

    @Nested
    @DisplayName("numeric_range operator")
    class NumericRangeTests {

        @Test
        @DisplayName("数值在范围内")
        void matchInRange() {
            Rule rule = buildRule(condRange("count", 3.0, 10.0));
            Map<String, String> f = fields(Map.entry("count", "5"));

            assertNotNull(matcher.matchRule(rule, f, "raw", "src", 1L));
        }

        @Test
        @DisplayName("数值超出范围")
        void noMatchOutOfRange() {
            Rule rule = buildRule(condRange("count", 3.0, 10.0));
            Map<String, String> f = fields(Map.entry("count", "15"));

            assertNull(matcher.matchRule(rule, f, "raw", "src", 1L));
        }

        @Test
        @DisplayName("仅设 min")
        void matchMinOnly() {
            Rule rule = buildRule(condRange("count", 5.0, null));
            Map<String, String> f = fields(Map.entry("count", "100"));

            assertNotNull(matcher.matchRule(rule, f, "raw", "src", 1L));
        }

        @Test
        @DisplayName("非数值字段 — 不匹配")
        void noMatchNonNumeric() {
            Rule rule = buildRule(condRange("count", 1.0, 10.0));
            Map<String, String> f = fields(Map.entry("count", "abc"));

            assertNull(matcher.matchRule(rule, f, "raw", "src", 1L));
        }
    }

    // ===== 多条件 AND 逻辑 =====

    @Test
    @DisplayName("多条件全部满足 — 匹配成功")
    void multiConditionAllMatch() {
        Rule rule = buildRule(
                cond("program", "equals", "sshd"),
                cond("message", "contains", "Failed password")
        );
        Map<String, String> f = fields(
                Map.entry("program", "sshd"),
                Map.entry("message", "Failed password for root")
        );

        assertNotNull(matcher.matchRule(rule, f, "raw", "src", 1L));
    }

    @Test
    @DisplayName("多条件有一个不满足 — 匹配失败")
    void multiConditionOneFails() {
        Rule rule = buildRule(
                cond("program", "equals", "sshd"),
                cond("message", "contains", "Failed password")
        );
        Map<String, String> f = fields(
                Map.entry("program", "sshd"),
                Map.entry("message", "Accepted publickey")
        );

        assertNull(matcher.matchRule(rule, f, "raw", "src", 1L));
    }

    // ===== 字段提取 =====

    @Test
    @DisplayName("字段提取 — 从 message 提取 source_ip")
    void extractField() {
        Rule rule = buildRule(cond("program", "equals", "sshd"));

        FieldExtraction ext = new FieldExtraction();
        ext.setField("source_ip");
        ext.setFrom("message");
        ext.setRegex("from (\\d+\\.\\d+\\.\\d+\\.\\d+)");
        rule.getConditions().setExtract(List.of(ext));

        Map<String, String> f = fields(
                Map.entry("program", "sshd"),
                Map.entry("message", "Failed password for root from 10.0.0.5 port 22")
        );

        SecurityEvent event = matcher.matchRule(rule, f, "raw", "src", 1L);
        assertNotNull(event);
        assertEquals("10.0.0.5", event.getExtractedFields().get("source_ip"));
    }

    // ===== MITRE 映射 =====

    @Test
    @DisplayName("MITRE ATT&CK 标签正确传递")
    void mitreMapping() {
        Rule rule = buildRule(cond("program", "equals", "sshd"));

        Rule.MitreMapping mitre = new Rule.MitreMapping();
        mitre.setTactic(List.of("TA0006"));
        mitre.setTechnique(List.of("T1110", "T1110.001"));
        rule.setMitre(mitre);

        Map<String, String> f = fields(Map.entry("program", "sshd"));
        SecurityEvent event = matcher.matchRule(rule, f, "raw", "src", 1L);

        assertNotNull(event);
        assertEquals("TA0006", event.getMitreTactic());
        assertEquals("T1110,T1110.001", event.getMitreTechnique());
    }

    // ===== 边界情况 =====

    @Test
    @DisplayName("conditions 为 null — 返回 null")
    void nullConditions() {
        Rule rule = new Rule();
        rule.setRuleId(1);
        rule.setConditions(null);

        assertNull(matcher.matchRule(rule, new HashMap<>(), "raw", "src", 1L));
    }

    @Test
    @DisplayName("空字段 map — 返回 null")
    void emptyFields() {
        Rule rule = buildRule(cond("program", "equals", "sshd"));
        assertNull(matcher.matchRule(rule, new HashMap<>(), "raw", "src", 1L));
    }
}
