package com.secguard.server.controller;

import com.secguard.common.dto.ApiResponse;
import com.secguard.server.engine.RuleEngine;
import com.secguard.server.engine.model.Rule;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 规则管理控制器
 *
 * 提供规则列表查看和热重载功能。
 */
@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleEngine ruleEngine;

    /**
     * 获取所有已加载的规则
     * GET /api/rules
     */
    @GetMapping
    public ApiResponse<Collection<Rule>> listRules() {
        return ApiResponse.ok(ruleEngine.getAllRules());
    }

    /**
     * 获取规则统计
     * GET /api/rules/stats
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Collection<Rule> rules = ruleEngine.getAllRules();

        Map<String, Long> byCategory = rules.stream()
                .collect(Collectors.groupingBy(Rule::getCategory, Collectors.counting()));

        Map<String, Long> bySeverity = rules.stream()
                .collect(Collectors.groupingBy(r -> {
                    int level = r.getLevel();
                    if (level >= 12) return "CRITICAL";
                    if (level >= 8) return "HIGH";
                    if (level >= 5) return "MEDIUM";
                    return "LOW";
                }, Collectors.counting()));

        return ApiResponse.ok(Map.of(
                "total", rules.size(),
                "byCategory", byCategory,
                "bySeverity", bySeverity
        ));
    }

    /**
     * 热重载规则
     * POST /api/rules/reload
     */
    @PostMapping("/reload")
    public ApiResponse<Map<String, Object>> reloadRules() {
        ruleEngine.reloadRules();
        return ApiResponse.ok(Map.of(
                "loaded", ruleEngine.getLoadedRuleCount(),
                "message", "Rules reloaded successfully"
        ));
    }
}
