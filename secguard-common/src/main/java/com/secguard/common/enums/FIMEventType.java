package com.secguard.common.enums;

/**
 * FIM 文件变更类型
 */
public enum FIMEventType {
    ADDED("新增"),
    MODIFIED("修改"),
    DELETED("删除");

    private final String label;

    FIMEventType(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
