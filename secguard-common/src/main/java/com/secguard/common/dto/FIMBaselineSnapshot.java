package com.secguard.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * FIM 基线快照（Agent 一次性上报全部监控文件的当前状态）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FIMBaselineSnapshot {
    /** 快照版本（Agent 端递增） */
    private int version;
    /** 基线条目列表 */
    private List<FIMBaselineEntry> entries;
}
