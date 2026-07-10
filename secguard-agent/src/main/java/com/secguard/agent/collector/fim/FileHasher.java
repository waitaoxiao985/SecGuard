package com.secguard.agent.collector.fim;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 文件哈希计算器
 *
 * 使用 SHA-256 计算文件摘要，用于文件完整性比对。
 * 对大文件采用流式读取，避免内存溢出。
 */
@Slf4j
public class FileHasher {

    private static final int BUFFER_SIZE = 8192;

    /**
     * 计算文件的 SHA-256 哈希值
     *
     * @param filePath 文件路径
     * @return 64 位十六进制字符串，出错返回 null
     */
    public static String sha256(Path filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hash = digest.digest();
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available: {}", e.getMessage());
            return null;
        } catch (IOException e) {
            log.warn("Failed to hash file {}: {}", filePath, e.getMessage());
            return null;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
