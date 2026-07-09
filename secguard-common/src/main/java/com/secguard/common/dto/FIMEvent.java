package com.secguard.common.dto;

import com.secguard.common.enums.FIMEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * FIM 文件完整性事件（Agent -> Server）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FIMEvent {

    /** 事件时间戳 */
    private Instant timestamp;

    /** 变更文件路径 */
    private String filePath;

    /** 变更类型 */
    private FIMEventType eventType;

    /** 当前 SHA-256 哈希 */
    private String sha256;

    /** 变更前哈希 */
    private String previousSha256;

    /** 文件大小 */
    private Long fileSize;

    /** 文件权限 */
    private String permissions;

    /** 文件属主 */
    private String owner;
}
