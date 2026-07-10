package com.secguard.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secguard.common.dto.LogEvent;
import com.secguard.server.engine.RuleEngine;
import com.secguard.server.entity.LogEventEntity;
import com.secguard.server.repository.AgentRepository;
import com.secguard.server.repository.LogEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * 日志事件服务
 *
 * W3: 接收 Agent 上报的日志事件并持久化
 * W4: 持久化后送入规则引擎进行匹配，触发告警
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogEventService {

    private final LogEventRepository logEventRepository;
    private final AgentRepository agentRepository;
    private final ObjectMapper objectMapper;
    private final RuleEngine ruleEngine;
    private final TransactionTemplate transactionTemplate;

    /**
     * 批量接收并处理日志事件
     *
     * 1. 在独立事务中存储到 sg_log_event 表（确保日志不会因规则引擎异常丢失）
     * 2. 事务提交后送入规则引擎匹配 → 触发告警
     *
     * @param agentKey Agent 密钥
     * @param events   日志事件列表
     * @return 实际存储的事件数
     */
    public int ingestEvents(String agentKey, List<LogEvent> events) {
        Long agentId = agentRepository.findByAgentKey(agentKey)
                .map(a -> a.getId())
                .orElse(null);

        if (agentId == null) {
            log.warn("Received log events from unknown agent: {}", agentKey);
            return 0;
        }

        // 1. 在独立事务中持久化日志（事务提交后才继续）
        List<LogEventEntity> entities = events.stream()
                .map(event -> toEntity(agentId, event))
                .toList();

        transactionTemplate.executeWithoutResult(status -> {
            logEventRepository.saveAll(entities);
        });
        log.info("Ingested {} log events from agent {} (id={})", events.size(), agentKey, agentId);

        // 2. 规则引擎处理（独立于持久化事务，失败不影响已存储的日志）
        for (LogEvent event : events) {
            try {
                Map<String, String> fields = event.getFields();
                if (fields == null) fields = Map.of("message", event.getRawLog());

                ruleEngine.processEvent(
                        fields,
                        event.getRawLog(),
                        event.getSource(),
                        agentId
                );
            } catch (Exception e) {
                log.warn("Rule engine error for event: {}", e.getMessage());
            }
        }

        return entities.size();
    }

    /**
     * 分页查询日志事件
     */
    public Page<LogEventEntity> queryLogs(Long agentId, String category, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (agentId != null && category != null && !category.isBlank()) {
            return logEventRepository.findByAgentIdAndCategory(agentId, category, pageable);
        } else if (agentId != null) {
            return logEventRepository.findByAgentId(agentId, pageable);
        } else if (category != null && !category.isBlank()) {
            return logEventRepository.findByCategory(category, pageable);
        }
        return logEventRepository.findAll(pageable);
    }

    /**
     * 统计日志总量
     */
    public long countTotal() {
        return logEventRepository.count();
    }

    private LogEventEntity toEntity(Long agentId, LogEvent event) {
        String fieldsJson = null;
        if (event.getFields() != null && !event.getFields().isEmpty()) {
            try {
                fieldsJson = objectMapper.writeValueAsString(event.getFields());
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize fields: {}", e.getMessage());
                fieldsJson = "{}";
            }
        }

        LocalDateTime eventTime = null;
        if (event.getTimestamp() != null) {
            eventTime = LocalDateTime.ofInstant(event.getTimestamp(), ZoneId.systemDefault());
        }

        return LogEventEntity.builder()
                .agentId(agentId)
                .source(event.getSource())
                .format(event.getFormat())
                .rawLog(event.getRawLog())
                .fields(fieldsJson)
                .category(event.getCategory() != null ? event.getCategory().name() : null)
                .eventTime(eventTime)
                .build();
    }
}
