package com.secguard.common.enums;

/**
 * 安全事件类别
 */
public enum EventCategory {
    AUTHENTICATION("认证事件", "SSH/登录/密码"),
    FIM("文件完整性", "文件变更/权限修改"),
    NETWORK("网络事件", "端口扫描/异常连接"),
    SYSTEM("系统事件", "进程/服务/内核"),
    WEB("Web服务器", "HTTP访问/错误"),
    FIREWALL("防火墙", "规则命中/拒绝"),
    INVENTORY("资产变更", "软件/端口/系统信息"),
    VULNERABILITY("漏洞检测", "CVE匹配");

    private final String label;
    private final String description;

    EventCategory(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
}
