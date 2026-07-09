package com.secguard.common.enums;

/**
 * 告警处置状态
 */
public enum AlertStatus {
    OPEN("未处理"),
    ACKNOWLEDGED("已确认"),
    RESOLVED("已解决"),
    FALSE_POSITIVE("误报");

    private final String label;

    AlertStatus(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
