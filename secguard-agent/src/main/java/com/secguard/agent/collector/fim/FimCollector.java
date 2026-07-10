package com.secguard.agent.collector.fim;

import com.secguard.agent.config.AgentProperties;
import com.secguard.agent.registration.AgentRegistrar;
import com.secguard.agent.sender.EventSender;
import com.secguard.common.dto.FIMBaselineEntry;
import com.secguard.common.dto.FIMBaselineSnapshot;
import com.secguard.common.dto.FIMEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FIM 采集调度器
 *
 * 定期触发文件完整性扫描，将变更事件批量上报到 Server。
 * 首次运行时自动建立基线（所有文件视为初始状态，不产生告警）。
 *
 * 参考 LogCollector 模式：
 * - @Scheduled 定时触发
 * - 检查 Agent 注册状态
 * - 扫描 → 比对基线 → 批量发送 → 持久化基线
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FimCollector {

    private final AgentProperties properties;
    private final AgentRegistrar registrar;
    private final FimScanner fimScanner;
    private final FimBaseline fimBaseline;
    private final EventSender eventSender;

    private boolean baselineInitialized = false;
    private int baselineVersion = 0;

    @PostConstruct
    public void init() {
        // 初始化基线文件路径
        fimBaseline.init(properties.getAgent().getDataDir());
    }

    /**
     * 定时扫描任务
     *
     * 间隔从 secguard.fim.interval 读取（秒），转换为毫秒。
     * 初始延迟 15 秒（等 Agent 注册完成 + 日志采集稳定）。
     */
    @Scheduled(fixedDelayString = "${secguard.fim.interval:300}000", initialDelay = 15000)
    public void collect() {
        if (registrar.getAgentKey() == null) {
            log.debug("Agent not registered yet, skipping FIM scan");
            return;
        }

        List<String> fimPaths = properties.getFim().getPaths();
        if (fimPaths == null || fimPaths.isEmpty()) {
            return;
        }

        log.debug("FIM scan started, monitoring {} path(s), baseline size: {}",
                fimPaths.size(), fimBaseline.size());

        // 执行扫描
        List<FIMEvent> events = fimScanner.scan();

        // 首次扫描：建立基线，不上报变更事件，但上报基线快照到 Server
        if (!baselineInitialized) {
            baselineInitialized = true;
            baselineVersion++;
            fimBaseline.saveToDisk();
            log.info("FIM baseline initialized: {} file(s) cataloged", fimBaseline.size());
            uploadBaselineSnapshot();
            return;
        }

        // 上报变更事件
        if (!events.isEmpty()) {
            boolean success = eventSender.sendFimEvents(events, registrar.getAgentKey());
            if (success) {
                baselineVersion++;
                fimBaseline.saveToDisk();
                log.info("FIM scan complete: {} change(s) detected and reported", events.size());
                // 变更上报成功后同步基线快照到 Server
                uploadBaselineSnapshot();
            } else {
                log.warn("FIM scan: {} change(s) detected but failed to report (will retry next scan)",
                        events.size());
            }
        } else {
            log.debug("FIM scan complete: no changes detected");
        }
    }

    /**
     * 构建并上传基线快照到 Server
     */
    private void uploadBaselineSnapshot() {
        try {
            Map<String, FimBaseline.FileEntry> snapshot = fimBaseline.snapshot();
            List<FIMBaselineEntry> entries = new ArrayList<>(snapshot.size());
            for (Map.Entry<String, FimBaseline.FileEntry> entry : snapshot.entrySet()) {
                FimBaseline.FileEntry fe = entry.getValue();
                entries.add(FIMBaselineEntry.builder()
                        .filePath(entry.getKey())
                        .sha256(fe.getSha256())
                        .fileSize(fe.getFileSize())
                        .permissions(fe.getPermissions())
                        .owner(fe.getOwner())
                        .build());
            }

            FIMBaselineSnapshot fimSnapshot = FIMBaselineSnapshot.builder()
                    .version(baselineVersion)
                    .entries(entries)
                    .build();

            eventSender.sendBaselineSnapshot(fimSnapshot, registrar.getAgentKey());
        } catch (Exception e) {
            log.warn("Failed to upload baseline snapshot: {}", e.getMessage());
        }
    }
}
