package com.secguard.agent.collector.inventory;

import com.secguard.agent.config.AgentProperties;
import com.secguard.agent.registration.AgentRegistrar;
import com.secguard.agent.sender.EventSender;
import com.secguard.common.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 资产采集调度器
 *
 * 定期调用各采集器收集系统信息、软件列表、端口列表、网络接口，
 * 打包成 InventorySnapshot 上报到 Server。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryCollector {

    private final AgentProperties properties;
    private final AgentRegistrar registrar;
    private final SystemInfoCollector systemInfoCollector;
    private final SoftwareCollector softwareCollector;
    private final PortCollector portCollector;
    private final NetworkCollector networkCollector;
    private final EventSender eventSender;

    /**
     * 定时采集任务
     *
     * 间隔从 secguard.inventory.interval 读取（秒），转换为毫秒。
     * 初始延迟 25 秒（等 Agent 注册完成 + 其他采集稳定）。
     */
    @Scheduled(fixedDelayString = "${secguard.inventory.interval:3600}000", initialDelay = 25000)
    public void collect() {
        if (!properties.getInventory().isEnabled()) {
            log.debug("Inventory collection disabled");
            return;
        }

        if (registrar.getAgentKey() == null) {
            log.debug("Agent not registered yet, skipping inventory collection");
            return;
        }

        log.debug("Inventory collection started");
        long startTime = System.currentTimeMillis();

        try {
            Instant now = Instant.now();

            // 采集各维度数据
            AssetSystemDTO system = systemInfoCollector.collect();
            List<AssetSoftwareDTO> software = softwareCollector.collect();
            List<AssetPortDTO> ports = portCollector.collect();
            List<AssetNetworkDTO> networks = networkCollector.collect();

            // 打包快照
            InventorySnapshot snapshot = InventorySnapshot.builder()
                    .system(system)
                    .software(software)
                    .ports(ports)
                    .networks(networks)
                    .collectedAt(now)
                    .build();

            // 上报到 Server
            boolean success = eventSender.sendInventorySnapshot(snapshot, registrar.getAgentKey());
            long elapsed = System.currentTimeMillis() - startTime;

            if (success) {
                log.info("Inventory collection complete in {}ms: system={}, software={}, ports={}, networks={}",
                        elapsed, system.getHostname(), software.size(), ports.size(), networks.size());
            } else {
                log.warn("Inventory collection complete in {}ms but failed to report (will retry next cycle)",
                        elapsed);
            }

        } catch (Exception e) {
            log.error("Inventory collection failed: {}", e.getMessage(), e);
        }
    }
}
