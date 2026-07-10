package com.secguard.server.service;

import com.secguard.common.dto.FIMEvent;
import com.secguard.server.entity.FimEventEntity;
import com.secguard.server.repository.AgentRepository;
import com.secguard.server.repository.FimEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * FIM 事件服务
 *
 * 接收 Agent 上报的文件完整性变更事件并持久化到 sg_fim_event 表。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FimEventService {

    private final FimEventRepository fimEventRepository;
    private final AgentRepository agentRepository;

    /**
     * 批量接收并持久化 FIM 事件
     *
     * @param agentKey Agent 密钥
     * @param events   FIM 事件列表
     * @return 实际存储的事件数
     */
    @Transactional
    public int ingestEvents(String agentKey, List<FIMEvent> events) {
        Long agentId = agentRepository.findByAgentKey(agentKey)
                .map(a -> a.getId())
                .orElse(null);

        if (agentId == null) {
            log.warn("Received FIM events from unknown agent: {}", agentKey);
            return 0;
        }

        List<FimEventEntity> entities = events.stream()
                .map(event -> toEntity(agentId, event))
                .toList();
        fimEventRepository.saveAll(entities);
        log.info("Ingested {} FIM events from agent {} (id={})", events.size(), agentKey, agentId);

        return entities.size();
    }

    /**
     * 分页查询 FIM 事件
     */
    public Page<FimEventEntity> queryEvents(Long agentId, String eventType, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (agentId != null && eventType != null && !eventType.isBlank()) {
            return fimEventRepository.findByAgentIdAndEventType(agentId, eventType, pageable);
        } else if (agentId != null) {
            return fimEventRepository.findByAgentId(agentId, pageable);
        } else if (eventType != null && !eventType.isBlank()) {
            return fimEventRepository.findByEventType(eventType, pageable);
        }
        return fimEventRepository.findAll(pageable);
    }

    private FimEventEntity toEntity(Long agentId, FIMEvent event) {
        return FimEventEntity.builder()
                .agentId(agentId)
                .filePath(event.getFilePath())
                .eventType(event.getEventType().name())
                .sha256Hash(event.getSha256())
                .previousHash(event.getPreviousSha256())
                .fileSize(event.getFileSize())
                .permissions(event.getPermissions())
                .owner(event.getOwner())
                .build();
    }
}
