package com.secguard.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SecGuard Agent 启动入口
 *
 * 功能概述：
 * - 日志采集（LogFileTailer）
 * - 文件完整性监控（FIM）
 * - 主机资产采集
 * - 心跳上报
 * - 事件批量发送到 Server
 */
@SpringBootApplication
@EnableScheduling
public class SecGuardAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecGuardAgentApplication.class, args);
    }
}
