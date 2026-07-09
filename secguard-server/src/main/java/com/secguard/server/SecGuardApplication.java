package com.secguard.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SecGuard Server 启动入口
 *
 * 功能概述：
 * - 接收 Agent 注册/心跳/事件上报
 * - YAML 规则引擎实时分析日志
 * - FIM 文件完整性监控
 * - 主机资产管理与漏洞检测
 * - WebSocket 实时告警推送
 */
@SpringBootApplication
@EnableScheduling
public class SecGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecGuardApplication.class, args);
    }
}
