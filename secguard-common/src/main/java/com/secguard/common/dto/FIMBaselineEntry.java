package com.secguard.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FIM 基线条目（Agent -> Server 上报用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FIMBaselineEntry {
    private String filePath;
    private String sha256;
    private Long fileSize;
    private String permissions;
    private String owner;
}
