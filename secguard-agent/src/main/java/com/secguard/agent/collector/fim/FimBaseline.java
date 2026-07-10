package com.secguard.agent.collector.fim;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FIM 文件基线管理器
 *
 * 维护内存中的文件哈希基线（ConcurrentHashMap），支持：
 * - 查询某路径的已知哈希
 * - 更新/添加基线条目
 * - 删除已不存在的文件条目
 * - 持久化到 .secguard/fim-baseline.json（启动时自动加载）
 */
@Component
@Slf4j
public class FimBaseline {

    private final Map<String, FileEntry> baseline = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private Path baselineFile;

    public FimBaseline() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 初始化：设置持久化文件路径并从磁盘加载已有基线
     */
    public void init(String dataDir) {
        this.baselineFile = Paths.get(dataDir, "fim-baseline.json");
        loadFromDisk();
    }

    /**
     * 获取文件的已知哈希
     */
    public FileEntry get(String filePath) {
        return baseline.get(filePath);
    }

    /**
     * 文件是否存在于基线中
     */
    public boolean contains(String filePath) {
        return baseline.containsKey(filePath);
    }

    /**
     * 添加或更新基线条目
     */
    public void put(String filePath, String sha256, long fileSize, String permissions, String owner) {
        baseline.put(filePath, new FileEntry(sha256, fileSize, permissions, owner));
    }

    /**
     * 移除基线条目（文件已被删除）
     */
    public FileEntry remove(String filePath) {
        return baseline.remove(filePath);
    }

    /**
     * 获取所有基线条目的快照（用于遍历检测删除）
     */
    public Map<String, FileEntry> snapshot() {
        return Map.copyOf(baseline);
    }

    /**
     * 当前基线中的文件数量
     */
    public int size() {
        return baseline.size();
    }

    /**
     * 持久化基线到磁盘
     */
    public void saveToDisk() {
        if (baselineFile == null) return;
        try {
            Files.createDirectories(baselineFile.getParent());
            objectMapper.writeValue(baselineFile.toFile(), baseline);
            log.debug("FIM baseline saved: {} entries", baseline.size());
        } catch (IOException e) {
            log.warn("Failed to save FIM baseline: {}", e.getMessage());
        }
    }

    /**
     * 从磁盘加载基线
     */
    private void loadFromDisk() {
        if (baselineFile == null || !Files.exists(baselineFile)) {
            log.info("No existing FIM baseline found, starting fresh");
            return;
        }
        try {
            Map<String, FileEntry> loaded = objectMapper.readValue(
                    baselineFile.toFile(),
                    new TypeReference<Map<String, FileEntry>>() {}
            );
            baseline.putAll(loaded);
            log.info("Loaded FIM baseline from disk: {} entries", baseline.size());
        } catch (IOException e) {
            log.warn("Failed to load FIM baseline, starting fresh: {}", e.getMessage());
        }
    }

    /**
     * 基线中的文件条目
     */
    @Data
    public static class FileEntry {
        private String sha256;
        private long fileSize;
        private String permissions;
        private String owner;

        public FileEntry() {}

        public FileEntry(String sha256, long fileSize, String permissions, String owner) {
            this.sha256 = sha256;
            this.fileSize = fileSize;
            this.permissions = permissions;
            this.owner = owner;
        }
    }
}
