package com.secguard.agent.collector.log;

import com.secguard.agent.config.AgentProperties;
import com.secguard.agent.registration.AgentRegistrar;
import com.secguard.agent.sender.EventSender;
import com.secguard.common.dto.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

/**
 * 日志采集调度器
 *
 * 定期扫描配置的日志路径，对每个文件创建 LogFileTailer，
 * 读取新增行 → 解析 → 封装为 LogEvent → 批量发送到 Server。
 *
 * 支持通配符路径（如 /var/log/*.log）。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LogCollector {

    private final AgentProperties properties;
    private final AgentRegistrar registrar;
    private final LogParser logParser;
    private final EventSender eventSender;

    /** 文件路径 -> Tailer 的映射 */
    private final Map<String, LogFileTailer> tailers = new HashMap<>();

    @Scheduled(fixedDelayString = "${secguard.log.interval:5}000", initialDelay = 10000)
    public void collect() {
        if (registrar.getAgentKey() == null) {
            log.debug("Agent not registered yet, skipping log collection");
            return;
        }

        List<String> paths = properties.getLog().getPaths();
        if (paths == null || paths.isEmpty()) {
            return;
        }

        Path dataDir = Paths.get(properties.getAgent().getDataDir());
        List<LogEvent> events = new ArrayList<>();
        int maxBatch = properties.getLog().getMaxBatchSize();

        for (String pathPattern : paths) {
            try {
                // 处理通配符路径
                List<Path> resolvedPaths = resolvePath(pathPattern);

                for (Path filePath : resolvedPaths) {
                    try {
                        collectFromFile(filePath, dataDir, events, maxBatch);
                    } catch (Exception e) {
                        log.warn("Error collecting from {}: {}", filePath, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("Error resolving path pattern {}: {}", pathPattern, e.getMessage());
            }
        }

        // 批量发送
        if (!events.isEmpty()) {
            boolean success = eventSender.sendLogEvents(events, registrar.getAgentKey());
            if (success) {
                // 发送成功后保存 offset
                tailers.values().forEach(tailer -> {
                    try {
                        tailer.saveOffset();
                    } catch (IOException e) {
                        log.warn("Failed to save offset for {}: {}", tailer.getFilePath(), e.getMessage());
                    }
                });
            }
        }
    }

    private void collectFromFile(Path filePath, Path dataDir, List<LogEvent> events, int maxBatch)
            throws IOException {
        if (!Files.exists(filePath)) return;

        String key = filePath.toAbsolutePath().normalize().toString();
        LogFileTailer tailer = tailers.computeIfAbsent(key, k -> {
            try {
                return new LogFileTailer(filePath, dataDir);
            } catch (IOException e) {
                log.error("Failed to create tailer for {}: {}", filePath, e.getMessage());
                return null;
            }
        });

        if (tailer == null) return;

        List<String> lines = tailer.readNewLines();
        for (String line : lines) {
            if (events.size() >= maxBatch) break;

            LogParser.ParseResult parsed = logParser.parse(line, key);

            LogEvent event = LogEvent.builder()
                    .timestamp(Instant.now())
                    .rawLog(line)
                    .source(key)
                    .format(parsed.format())
                    .fields(parsed.fields())
                    .category(logParser.inferCategory(line, key))
                    .build();
            events.add(event);
        }

        if (!lines.isEmpty()) {
            log.debug("Collected {} lines from {}", lines.size(), filePath.getFileName());
        }
    }

    /**
     * 解析路径模式（支持通配符 *）
     */
    private List<Path> resolvePath(String pathPattern) {
        Path path = Paths.get(pathPattern);

        if (pathPattern.contains("*")) {
            // 通配符：取父目录 + glob 匹配
            Path parent = path.getParent();
            String pattern = path.getFileName().toString();
            if (parent == null) parent = Paths.get(".");

            if (!Files.exists(parent)) return List.of();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent, pattern)) {
                List<Path> result = new ArrayList<>();
                for (Path p : stream) {
                    if (Files.isRegularFile(p)) {
                        result.add(p);
                    }
                }
                return result;
            } catch (IOException e) {
                log.warn("Failed to resolve wildcard pattern: {}", pathPattern);
                return List.of();
            }
        }

        return Files.exists(path) ? List.of(path) : List.of();
    }

    public int getTailerCount() {
        return tailers.size();
    }
}
