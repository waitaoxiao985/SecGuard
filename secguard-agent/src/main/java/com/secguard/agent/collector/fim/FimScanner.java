package com.secguard.agent.collector.fim;

import com.secguard.agent.config.AgentProperties;
import com.secguard.common.dto.FIMEvent;
import com.secguard.common.enums.FIMEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.util.*;

/**
 * FIM 文件完整性扫描器
 *
 * 定期扫描配置的监控目录，与内存基线比对，检测文件新增/修改/删除。
 * 参考 Wazuh FIM 模块设计：
 * - 递归遍历目录树
 * - SHA-256 哈希比对（大文件可跳过哈希只比对大小）
 * - 权限变更检测（Unix 系统）
 * - 增量变更检测（只上报变化的部分）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FimScanner {

    private final AgentProperties properties;
    private final FimBaseline baseline;

    /**
     * 执行一次完整扫描，返回变更事件列表
     */
    public List<FIMEvent> scan() {
        List<String> paths = properties.getFim().getPaths();
        if (paths == null || paths.isEmpty()) {
            return List.of();
        }

        long maxSizeBytes = (long) properties.getFim().getMaxFileSizeMb() * 1024 * 1024;
        List<FIMEvent> events = new ArrayList<>();
        Set<String> scannedPaths = new HashSet<>();

        // 1. 扫描所有配置的目录
        for (String dirPath : paths) {
            Path dir = Paths.get(dirPath);
            if (!Files.exists(dir)) {
                log.debug("FIM monitored path does not exist: {}", dirPath);
                continue;
            }

            if (Files.isRegularFile(dir)) {
                // 监控单个文件
                scanFile(dir, maxSizeBytes, events, scannedPaths);
            } else if (Files.isDirectory(dir)) {
                // 递归扫描目录
                scanDirectory(dir, maxSizeBytes, events, scannedPaths);
            }
        }

        // 2. 检测已删除的文件（基线中存在但本次扫描未发现）
        detectDeletedFiles(events, scannedPaths);

        return events;
    }

    /**
     * 递归扫描目录
     */
    private void scanDirectory(Path dir, long maxSizeBytes,
                                List<FIMEvent> events, Set<String> scannedPaths) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    scanFile(file, maxSizeBytes, events, scannedPaths);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.debug("FIM: cannot access {}: {}", file, exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("FIM: failed to walk directory {}: {}", dir, e.getMessage());
        }
    }

    /**
     * 扫描单个文件，与基线比对
     */
    private void scanFile(Path file, long maxSizeBytes,
                           List<FIMEvent> events, Set<String> scannedPaths) {
        String filePath = file.toAbsolutePath().normalize().toString();
        scannedPaths.add(filePath);

        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            if (!attrs.isRegularFile()) return;

            long fileSize = attrs.size();
            String permissions = getPermissions(file);
            String owner = getOwner(file);

            // 计算哈希（超过大小限制则用 "size:<bytes>" 代替）
            String currentHash;
            if (fileSize <= maxSizeBytes) {
                currentHash = FileHasher.sha256(file);
                if (currentHash == null) return; // 哈希失败，跳过
            } else {
                currentHash = "size:" + fileSize;
            }

            FimBaseline.FileEntry existing = baseline.get(filePath);

            if (existing == null) {
                // 新文件
                baseline.put(filePath, currentHash, fileSize, permissions, owner);
                events.add(buildEvent(filePath, FIMEventType.ADDED, currentHash, null, fileSize, permissions, owner));
                log.debug("FIM: new file detected: {}", filePath);
            } else if (!currentHash.equals(existing.getSha256())) {
                // 文件内容变更
                String previousHash = existing.getSha256();
                baseline.put(filePath, currentHash, fileSize, permissions, owner);
                events.add(buildEvent(filePath, FIMEventType.MODIFIED, currentHash, previousHash, fileSize, permissions, owner));
                log.debug("FIM: file modified: {}", filePath);
            } else if (!Objects.equals(permissions, existing.getPermissions())) {
                // 权限变更（哈希相同但权限不同）
                baseline.put(filePath, currentHash, fileSize, permissions, owner);
                events.add(buildEvent(filePath, FIMEventType.MODIFIED, currentHash, existing.getSha256(), fileSize, permissions, owner));
                log.debug("FIM: permissions changed: {}", filePath);
            }
            // 否则文件无变化，不产生事件

        } catch (IOException e) {
            log.debug("FIM: error scanning {}: {}", filePath, e.getMessage());
        }
    }

    /**
     * 检测已删除的文件
     */
    private void detectDeletedFiles(List<FIMEvent> events, Set<String> scannedPaths) {
        Map<String, FimBaseline.FileEntry> snapshot = baseline.snapshot();
        for (Map.Entry<String, FimBaseline.FileEntry> entry : snapshot.entrySet()) {
            String path = entry.getKey();
            // 只检查本次扫描范围内的路径（属于已配置目录下的文件）
            if (!scannedPaths.contains(path) && isUnderMonitoredPath(path)) {
                FimBaseline.FileEntry removed = baseline.remove(path);
                events.add(buildEvent(path, FIMEventType.DELETED, null,
                        removed != null ? removed.getSha256() : null,
                        removed != null ? removed.getFileSize() : 0,
                        null, null));
                log.debug("FIM: file deleted: {}", path);
            }
        }
    }

    /**
     * 判断路径是否属于某个监控目录
     */
    private boolean isUnderMonitoredPath(String filePath) {
        for (String dirPath : properties.getFim().getPaths()) {
            if (filePath.startsWith(Paths.get(dirPath).toAbsolutePath().normalize().toString())) {
                return true;
            }
        }
        return false;
    }

    private FIMEvent buildEvent(String filePath, FIMEventType type, String sha256,
                                 String previousSha256, long fileSize,
                                 String permissions, String owner) {
        return FIMEvent.builder()
                .timestamp(Instant.now())
                .filePath(filePath)
                .eventType(type)
                .sha256(sha256)
                .previousSha256(previousSha256)
                .fileSize(fileSize)
                .permissions(permissions)
                .owner(owner)
                .build();
    }

    /**
     * 获取文件权限（跨平台）
     */
    private String getPermissions(Path file) {
        try {
            PosixFileAttributes posix = Files.readAttributes(file, PosixFileAttributes.class);
            return posix.permissions().toString();
        } catch (UnsupportedOperationException | IOException e) {
            // Windows 或不支持 POSIX，返回文件大小标记
            try {
                return "rw-" + (Files.isWritable(file) ? "w" : "-");
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * 获取文件属主
     */
    private String getOwner(Path file) {
        try {
            UserPrincipal owner = Files.getOwner(file);
            return owner.getName();
        } catch (IOException e) {
            return null;
        }
    }
}
