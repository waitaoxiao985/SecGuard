package com.secguard.common.dto;

import com.secguard.common.enums.AgentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 列表查询参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentListQuery {

    /** 页码（从 0 开始） */
    private Integer page;

    /** 每页大小 */
    private Integer size;

    /** 按状态过滤 */
    private AgentStatus status;

    /** 关键词搜索（名称/IP/主机名） */
    private String keyword;
}
