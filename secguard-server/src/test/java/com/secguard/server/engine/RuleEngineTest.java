package com.secguard.server.engine;

import com.secguard.server.engine.model.Rule;
import com.secguard.server.engine.model.SecurityEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RuleEngine 单元测试（手动 stub，无 Mockito）
 *
 * RuleEngine 构造器接受具体类型，因此用内部类继承并 override 关键方法。
 */
class RuleEngineTest {

    private FakeRuleLoader ruleLoader;
    private FakeRuleMatcher ruleMatcher;
    private FakeAlertService alertService;
    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleLoader = new FakeRuleLoader();
        ruleMatcher = new FakeRuleMatcher();
        alertService = new FakeAlertService();
        ruleEngine = new RuleEngine(ruleLoader, ruleMatcher, alertService);
    }

    @Test
    @DisplayName("处理事件 — 无规则匹配时返回空列表")
    void processEvent_noMatch() {
        Rule rule = new Rule();
        rule.setRuleId(1001);
        ruleLoader.fakeRules = List.of(rule);

        List<SecurityEvent> events = ruleEngine.processEvent(
                Map.of("program", "nginx"), "raw log", "/var/log/nginx", 1L);

        assertTrue(events.isEmpty());
        assertEquals(0, alertService.createdCount.get());
    }

    @Test
    @DisplayName("处理事件 — 一条规则匹配 → 创建告警")
    void processEvent_singleMatch() {
        Rule rule = new Rule();
        rule.setRuleId(5710);
        ruleLoader.fakeRules = List.of(rule);

        SecurityEvent mockEvent = SecurityEvent.builder().ruleId(5710).ruleName("SSH Brute Force").build();
        ruleMatcher.fakeResults.put(5710, mockEvent);

        List<SecurityEvent> events = ruleEngine.processEvent(
                Map.of("program", "sshd"), "Failed password", "/var/log/auth.log", 1L);

        assertEquals(1, events.size());
        assertEquals(5710, events.get(0).getRuleId());
        assertEquals(1, alertService.createdCount.get());
    }

    @Test
    @DisplayName("处理事件 — 多条规则匹配 → 多个告警")
    void processEvent_multiMatch() {
        Rule rule1 = new Rule();
        rule1.setRuleId(1001);
        Rule rule2 = new Rule();
        rule2.setRuleId(1002);
        ruleLoader.fakeRules = List.of(rule1, rule2);

        ruleMatcher.fakeResults.put(1001, SecurityEvent.builder().ruleId(1001).build());
        ruleMatcher.fakeResults.put(1002, SecurityEvent.builder().ruleId(1002).build());

        List<SecurityEvent> events = ruleEngine.processEvent(
                Map.of("program", "sshd"), "log", "src", 1L);

        assertEquals(2, events.size());
        assertEquals(2, alertService.createdCount.get());
    }

    @Test
    @DisplayName("获取规则数量")
    void getLoadedRuleCount() {
        ruleLoader.fakeRules = List.of(new Rule(), new Rule(), new Rule());
        assertEquals(3, ruleEngine.getLoadedRuleCount());
    }

    @Test
    @DisplayName("获取所有规则")
    void getAllRules() {
        Rule r = new Rule();
        r.setRuleId(1);
        ruleLoader.fakeRules = List.of(r);

        Collection<Rule> rules = ruleEngine.getAllRules();
        assertEquals(1, rules.size());
    }

    // ===== 手动 Fake 类 =====

    static class FakeRuleLoader extends RuleLoader {
        List<Rule> fakeRules = new ArrayList<>();
        boolean loaded = false;

        @Override
        public Collection<Rule> getAllRules() { return fakeRules; }

        @Override
        public int getRuleCount() { return fakeRules.size(); }

        @Override
        public void loadRules() { loaded = true; }
    }

    static class FakeRuleMatcher extends RuleMatcher {
        Map<Integer, SecurityEvent> fakeResults = new HashMap<>();

        @Override
        public SecurityEvent matchRule(Rule rule, Map<String, String> fields,
                                        String rawLog, String source, Long agentId) {
            return fakeResults.get(rule.getRuleId());
        }
    }

    static class FakeAlertService extends com.secguard.server.service.AlertService {
        AtomicInteger createdCount = new AtomicInteger(0);

        FakeAlertService() {
            super(null, null, null, new com.fasterxml.jackson.databind.ObjectMapper());
        }

        @Override
        public com.secguard.server.entity.Alert createFromEvent(SecurityEvent event) {
            createdCount.incrementAndGet();
            return com.secguard.server.entity.Alert.builder()
                    .id((long) createdCount.get())
                    .ruleName(event.getRuleName())
                    .build();
        }
    }
}
