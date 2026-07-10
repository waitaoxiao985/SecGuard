package com.secguard.server.engine;

import com.secguard.server.engine.model.Rule;
import com.secguard.server.engine.model.SecurityEvent;
import com.secguard.server.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 规则引擎（编排器）
 *
 * 核心处理流程：
 * 1. 接收日志事件（字段 + 原始内容）
 * 2. 遍历所有已加载规则，逐条匹配
 * 3. 匹配成功 → 生成 SecurityEvent → 创建告警 → WebSocket 推送
 *
 * 一条日志可能触发多条规则（多级告警）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RuleEngine {

    private final RuleLoader ruleLoader;
    private final RuleMatcher ruleMatcher;
    private final AlertService alertService;

    /**
     * 处理单条日志事件
     *
     * @param fields  解析后的字段
     * @param rawLog  原始日志
     * @param source  日志来源
     * @param agentId Agent ID
     * @return 触发的安全事件列表
     */
    public List<SecurityEvent> processEvent(Map<String, String> fields, String rawLog,
                                             String source, Long agentId) {
        Collection<Rule> rules = ruleLoader.getAllRules();
        List<SecurityEvent> events = new ArrayList<>();

        for (Rule rule : rules) {
            SecurityEvent event = ruleMatcher.matchRule(rule, fields, rawLog, source, agentId);
            if (event != null) {
                events.add(event);
                // 创建告警并推送
                alertService.createFromEvent(event);
                log.info("Rule [{}] {} matched: level={}, category={}",
                        rule.getRuleId(), rule.getName(), rule.getLevel(), rule.getCategory());
            }
        }

        if (!events.isEmpty()) {
            log.debug("Event from {} triggered {} alert(s)", source, events.size());
        }

        return events;
    }

    /**
     * 获取已加载的规则数量
     */
    public int getLoadedRuleCount() {
        return ruleLoader.getRuleCount();
    }

    /**
     * 重新加载规则
     */
    public void reloadRules() {
        ruleLoader.loadRules();
    }

    /**
     * 获取所有已加载的规则
     */
    public Collection<Rule> getAllRules() {
        return ruleLoader.getAllRules();
    }
}
