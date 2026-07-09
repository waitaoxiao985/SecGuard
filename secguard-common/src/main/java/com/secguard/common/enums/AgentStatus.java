package com.secguard.common.enums;

/**
 * Agent 在线状态
 */
public enum AgentStatus {
    ACTIVE("在线"),
    DISCONNECTED("离线"),
    PENDING("待确认"),
    DISABLED("已禁用");

    private final String label;

    AgentStatus(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
