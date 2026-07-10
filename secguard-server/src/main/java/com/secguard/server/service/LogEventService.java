package com.secguard.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secguard.common.dto.LogEvent;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * 日志事件服务
 *
 * W3 职责：接收 Agent 上报的日志事件并持久化到 sg_log_event 表。
 * W4 会在此基础上增加规则引擎消费（Redis Stream → 匹配规则 → 生成告警）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogEventService {

    private final LogEventRepository logEventRepository;
    private final AgentRepository agentRepository;
    private final ObjectMapper objectMapper;

    /**
     * 批量接收并存储日志事件
     *
     * @param agentKey Agent 密钥
     * @param events   日志事件列表
     * @return 实际存储的事件数
     */
    @Transactional
    public int ingestEvents(String agentKey, List<LogEvent> events) {
        // 根据 agentKey 查找 Agent
        Long agentId = agentRepository.findByAgentKey(agentKey)
                .map(a -> a.getId())
                .orElse(null);

        if (agentId == null) {
            log.warn("Received log events from unknown agent: {}", agentKey);
            return 0;
        }

        List<LogEventEntity> entities = events.stream()
                .map(event -> toEntity(agentId, event))
                .toList();

        logEventRepository.saveAll(entities);
        log.info("Ingested {} log events from agent {} (id={})", events.size(), agentKey, agentId);
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

    /**
     * LogEvent DTO → LogEventEntity
     */
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
