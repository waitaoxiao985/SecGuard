package com.secguard.common.enums;

/**
 * 告警严重等级，对标 Wazuh rule level (0-15)
 */
public enum Severity {
    LOW(1, 4, "低"),
    MEDIUM(5, 7, "中"),
    HIGH(8, 11, "高"),
    CRITICAL(12, 15, "严重");

    private final int minLevel;
    private final int maxLevel;
    private final String label;

    Severity(int minLevel, int maxLevel, String label) {
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.label = label;
    }

    public static Severity fromLevel(int level) {
        for (Severity s : values()) {
            if (level >= s.minLevel && level <= s.maxLevel) return s;
        }
        return LOW;
    }

    public int getMinLevel() { return minLevel; }
    public int getMaxLevel() { return maxLevel; }
    public String getLabel() { return label; }
}
