package com.secguard.agent.collector.log;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志文件尾随器（Offset-based File Tailing）
 *
 * 核心能力：
 * - 通过 offset 跟踪每个文件的读取位置，重启后可从断点续读
 * - 检测日志轮转（文件大小缩小 → 重置 offset 从头读）
 * - 逐行读取，不完整的末行保留到下次读取
 *
 * 设计参考 Linux tail -F 命令的行为。
 */
@Slf4j
public class LogFileTailer {

    private final Path filePath;
    private final Path offsetFile;
    private long offset;
    private long lastFileSize;
    private String partialLine = "";

    /**
     * @param filePath 被监控的日志文件路径
     * @param dataDir  存放 offset 文件的目录
     */
    public LogFileTailer(Path filePath, Path dataDir) throws IOException {
        this.filePath = filePath.toAbsolutePath().normalize();
        // offset 文件名：将路径中的特殊字符替换为下划线
        String safeName = this.filePath.toString()
                .replaceAll("[^a-zA-Z0-9]", "_") + ".offset";
        this.offsetFile = dataDir.resolve(safeName);

        // 恢复上次读取位置
        if (Files.exists(offsetFile)) {
            String content = Files.readString(offsetFile).trim();
            this.offset = Long.parseLong(content);
        } else {
            this.offset = 0;
        }

        this.lastFileSize = Files.exists(this.filePath) ? Files.size(this.filePath) : 0;
        log.debug("LogFileTailer init: file={}, offset={}, size={}", this.filePath, offset, lastFileSize);
    }

    /**
     * 读取新增的行
     *
     * @return 新读取的完整行列表（不含末尾换行符）
     */
    public List<String> readNewLines() throws IOException {
        if (!Files.exists(filePath)) {
            return List.of();
        }

        long currentSize = Files.size(filePath);

        // 轮转检测：文件变小 → 重置 offset 从头读
        if (currentSize < lastFileSize) {
            log.info("Log rotation detected: {} ({} -> {} bytes)", filePath, lastFileSize, currentSize);
            offset = 0;
            partialLine = "";
        }

        // 没有新数据
        if (currentSize <= offset) {
            lastFileSize = currentSize;
            return List.of();
        }

        List<String> lines = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            raf.seek(offset);

            // 读取新增部分的字节
            byte[] buffer = new byte[(int) (currentSize - offset)];
            int bytesRead = raf.read(buffer);

            if (bytesRead > 0) {
                String newData = partialLine + new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);

                // 按换行符分割
                String[] parts = newData.split("\n", -1);

                // 最后一段可能不完整，保留到下次
                partialLine = parts[parts.length - 1];

                // 前面的都是完整行
                for (int i = 0; i < parts.length - 1; i++) {
                    String line = parts[i].trim();
                    if (!line.isEmpty()) {
                        lines.add(line);
                    }
                }

                offset += bytesRead;
            }
        }

        lastFileSize = currentSize;
        return lines;
    }

    /**
     * 将当前 offset 持久化到磁盘
     */
    public void saveOffset() throws IOException {
        Files.writeString(offsetFile, String.valueOf(offset));
    }

    public Path getFilePath() {
        return filePath;
    }

    public long getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return String.format("LogFileTailer{file=%s, offset=%d}", filePath, offset);
    }
}
