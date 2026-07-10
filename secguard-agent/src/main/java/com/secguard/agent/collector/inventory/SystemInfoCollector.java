package com.secguard.agent.collector.inventory;

import com.secguard.common.dto.AssetSystemDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * 系统信息采集器
 *
 * 采集主机名、操作系统、内核版本、CPU 型号、核心数、物理内存、运行时间。
 * 跨平台：Windows 使用 WMI / 注册表，Linux 使用 /proc 文件系统。
 */
@Component
@Slf4j
public class SystemInfoCollector {

    private final boolean isWindows;

    public SystemInfoCollector() {
        this.isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public AssetSystemDTO collect() {
        String os = System.getProperty("os.name", "unknown");
        String osVersion = System.getProperty("os.version", "");
        String hostname = getHostname();
        String kernel = getKernelVersion();
        String cpuModel = getCpuModel();
        int cpuCores = Runtime.getRuntime().availableProcessors();
        long ramTotalMb = getTotalMemoryMb();
        long uptimeSeconds = getUptimeSeconds();

        return AssetSystemDTO.builder()
                .hostname(hostname)
                .os(isWindows ? "windows" : "linux")
                .osVersion(os + " " + osVersion)
                .kernel(kernel)
                .cpuModel(cpuModel)
                .cpuCores(cpuCores)
                .ramTotalMb(ramTotalMb)
                .uptimeSeconds(uptimeSeconds)
                .collectedAt(Instant.now())
                .build();
    }

    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return System.getenv("COMPUTERNAME") != null
                    ? System.getenv("COMPUTERNAME")
                    : System.getenv("HOSTNAME");
        }
    }

    private String getKernelVersion() {
        if (isWindows) {
            return execSingleLine("cmd", "/c", "ver");
        } else {
            return execSingleLine("uname", "-r");
        }
    }

    private String getCpuModel() {
        if (isWindows) {
            return execWmi("wmic cpu get name", "Name");
        } else {
            try {
                java.nio.file.Path procCpu = java.nio.file.Paths.get("/proc/cpuinfo");
                if (java.nio.file.Files.exists(procCpu)) {
                    for (String line : java.nio.file.Files.readAllLines(procCpu)) {
                        if (line.startsWith("model name")) {
                            int idx = line.indexOf(':');
                            if (idx >= 0) {
                                return line.substring(idx + 1).trim();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to read /proc/cpuinfo: {}", e.getMessage());
            }
            return "unknown";
        }
    }

    private long getTotalMemoryMb() {
        try {
            com.sun.management.OperatingSystemMXBean sunBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return sunBean.getTotalMemorySize() / (1024 * 1024);
        } catch (Exception e) {
            log.debug("Failed to get system memory via MXBean: {}", e.getMessage());
            return 0;
        }
    }

    private long getUptimeSeconds() {
        // JVM uptime as a proxy (system uptime requires OS-specific calls)
        return TimeUnit.MILLISECONDS.toSeconds(ManagementFactory.getRuntimeMXBean().getUptime());
    }

    /**
     * 执行命令并返回第一行非空内容
     */
    private String execSingleLine(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            Charset cs = isWindows ? Charset.forName("GBK") : Charset.defaultCharset();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), cs))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        p.waitFor(5, TimeUnit.SECONDS);
                        return line;
                    }
                }
            }
            p.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Command failed [{}]: {}", String.join(" ", command), e.getMessage());
        }
        return "";
    }

    /**
     * 执行 WMI 命令，跳过标题行，返回第一条数据
     */
    private String execWmi(String command, String skipHeader) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), Charset.forName("GBK")))) {
                String line;
                boolean headerSkipped = false;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (!headerSkipped) {
                        headerSkipped = true;
                        continue;
                    }
                    p.waitFor(5, TimeUnit.SECONDS);
                    return line;
                }
            }
            p.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("WMI command failed [{}]: {}", command, e.getMessage());
        }
        return "";
    }
}
