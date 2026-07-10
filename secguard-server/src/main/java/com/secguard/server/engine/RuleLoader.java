package com.secguard.server.engine;

import com.secguard.server.engine.model.FieldExtraction;
import com.secguard.server.engine.model.MatchCondition;
import com.secguard.server.engine.model.Rule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * YAML 规则加载器
 *
 * 从 classpath:rules/ 目录加载所有 .yml 规则文件，
 * 解析为 Rule 对象并缓存。支持运行时热重载。
 */
@Component
@Slf4j
public class RuleLoader {

    @Value("${secguard.engine.rules-path:classpath:rules/}")
    private String rulesPath;

    private volatile Map<Integer, Rule> rules = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadRules();
    }

    /**
     * 加载/重载所有规则文件
     */
    @SuppressWarnings("unchecked")
    public synchronized void loadRules() {
        Map<Integer, Rule> newRules = new ConcurrentHashMap<>();
        Yaml yaml = new Yaml();

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(rulesPath + "*.yml");

            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    Map<String, Object> root = yaml.load(is);
                    if (root == null) continue;

                    Object rulesObj = root.get("rules");
                    if (!(rulesObj instanceof List)) continue;

                    for (Object item : (List<?>) rulesObj) {
                        if (!(item instanceof Map)) continue;
                        Map<String, Object> map = (Map<String, Object>) item;

                        Rule rule = parseRule(map);
                        if (rule != null) {
                            newRules.put(rule.getRuleId(), rule);
                            log.debug("Loaded rule: [{}] {} (level={}, category={})",
                                    rule.getRuleId(), rule.getName(), rule.getLevel(), rule.getCategory());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to load rule file: {}", resource.getFilename(), e);
                }
            }

            // 原子替换引用：在全部规则加载完毕后才切换，避免热加载期间规则为空
            this.rules = newRules;
            log.info("Rule engine loaded {} rule(s) from {} file(s)", newRules.size(), resources.length);
        } catch (Exception e) {
            log.error("Failed to scan rules directory: {}", rulesPath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Rule parseRule(Map<String, Object> map) {
        try {
            Rule rule = new Rule();

            Object ruleIdObj = map.get("rule_id");
            if (ruleIdObj == null) return null;
            rule.setRuleId(((Number) ruleIdObj).intValue());

            rule.setName((String) map.get("name"));
            if (rule.getName() == null) return null;

            Object levelObj = map.get("level");
            rule.setLevel(levelObj instanceof Number ? ((Number) levelObj).intValue() : 5);

            rule.setCategory((String) map.getOrDefault("category", "system"));
            rule.setDescription((String) map.get("description"));

            // Parse MITRE mapping
            Object mitreObj = map.get("mitre");
            if (mitreObj instanceof Map) {
                Map<String, Object> mitreMap = (Map<String, Object>) mitreObj;
                Rule.MitreMapping mitre = new Rule.MitreMapping();
                mitre.setTactic(toStringList(mitreMap.get("tactic")));
                mitre.setTechnique(toStringList(mitreMap.get("technique")));
                rule.setMitre(mitre);
            }

            // Parse PCI DSS
            rule.setPciDss(toStringList(map.get("pci_dss")));

            // Parse conditions
            Object condObj = map.get("conditions");
            if (condObj instanceof Map) {
                Map<String, Object> condMap = (Map<String, Object>) condObj;
                Rule.Conditions conditions = new Rule.Conditions();

                // Parse field_match
                Object fmObj = condMap.get("field_match");
                if (fmObj instanceof List) {
                    List<MatchCondition> conditions1 = new ArrayList<>();
                    for (Object fmItem : (List<?>) fmObj) {
                        if (fmItem instanceof Map) {
                            Map<String, Object> fmMap = (Map<String, Object>) fmItem;
                            MatchCondition mc = new MatchCondition();
                            mc.setField((String) fmMap.get("field"));
                            mc.setOperator((String) fmMap.get("operator"));
                            mc.setValue(fmMap.get("value") != null ? fmMap.get("value").toString() : null);
                            if (fmMap.containsKey("min")) {
                                mc.setMin(((Number) fmMap.get("min")).doubleValue());
                            }
                            if (fmMap.containsKey("max")) {
                                mc.setMax(((Number) fmMap.get("max")).doubleValue());
                            }
                            conditions1.add(mc);
                        }
                    }
                    conditions.setFieldMatch(conditions1);
                }

                // Parse extract
                Object extObj = condMap.get("extract");
                if (extObj instanceof List) {
                    List<FieldExtraction> extractions = new ArrayList<>();
                    for (Object extItem : (List<?>) extObj) {
                        if (extItem instanceof Map) {
                            Map<String, Object> extMap = (Map<String, Object>) extItem;
                            FieldExtraction fe = new FieldExtraction();
                            fe.setField((String) extMap.get("field"));
                            fe.setFrom((String) extMap.get("from"));
                            fe.setRegex((String) extMap.get("regex"));
                            extractions.add(fe);
                        }
                    }
                    conditions.setExtract(extractions);
                }

                rule.setConditions(conditions);
            }

            return rule;
        } catch (Exception e) {
            log.warn("Failed to parse rule: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object obj) {
        if (obj == null) return null;
        if (obj instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) obj) {
                result.add(item != null ? item.toString() : null);
            }
            return result;
        }
        return List.of(obj.toString());
    }

    /**
     * 获取所有已加载的规则
     */
    public Collection<Rule> getAllRules() {
        return Collections.unmodifiableCollection(rules.values());
    }

    /**
     * 按规则 ID 获取
     */
    public Rule getRule(int ruleId) {
        return rules.get(ruleId);
    }

    /**
     * 获取已加载规则数量
     */
    public int getRuleCount() {
        return rules.size();
    }
}
