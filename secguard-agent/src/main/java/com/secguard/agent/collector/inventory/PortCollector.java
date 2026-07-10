package com.secguard.agent.collector.inventory;

import com.secguard.common.dto.AssetPortDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 开放端口采集器
 *
 * Windows: netstat -ano（解析输出）
 * Linux:   ss -tulnp（优先）或 netstat -tulnp
 */
@Component
@Slf4j
public class PortCollector {

    private final boolean isWindows;

    public PortCollector() {
        this.isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public List<AssetPortDTO> collect() {
        if (isWindows) {
            return collectWindows();
        } else {
            return collectLinux();
        }
    }

    private List<AssetPortDTO> collectWindows() {
        List<AssetPortDTO> result = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "netstat -ano");
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // netstat 输出: Proto  Local Address  Foreign Address  State  PID
            Pattern pattern = Pattern.compile(
                    "\\s*(TCP|UDP)\\s+(\\S+):(\\d+)\\s+\\S+\\s+(\\S+)\\s+(\\d+)");

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), Charset.forName("GBK")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher m = pattern.matcher(line);
                    if (m.find()) {
                        String protocol = m.group(1).toLowerCase();
                        int port = Integer.parseInt(m.group(3));
                        String state = m.group(4);
                        int pid = Integer.parseInt(m.group(5));

                        // 只收集 LISTEN 状态的 TCP 端口 + 所有 UDP 端口
                        if ("TCP".equalsIgnoreCase(m.group(1)) && !"LISTENING".equalsIgnoreCase(state)) {
                            continue;
                        }

                        String processName = resolveProcessName(pid);

                        result.add(AssetPortDTO.builder()
                                .protocol(protocol)
                                .localPort(port)
                                .state("TCP".equalsIgnoreCase(m.group(1)) ? "LISTEN" : state)
                                .processName(processName)
                                .processPid(pid)
                                .build());
                    }
                }
            }
            p.waitFor(15, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.warn("Failed to collect ports on Windows: {}", e.getMessage());
        }

        log.debug("Collected {} listening ports (Windows)", result.size());
        return result;
    }

    private List<AssetPortDTO> collectLinux() {
        List<AssetPortDTO> result = new ArrayList<>();
        try {
            // 优先使用 ss，回退到 netstat
            String[] cmd = java.nio.file.Files.exists(java.nio.file.Paths.get("/usr/bin/ss"))
                    ? new String[]{"ss", "-tulnp"}
                    : new String[]{"netstat", "-tulnp"};

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // ss 输出格式: Netid State Recv-Q Send-Q Local Address:Port Peer Address:Port Process
            Pattern pattern = Pattern.compile(
                    "\\s*(tcp|udp)\\s+\\S+\\s+\\S+\\s+\\S+\\s+\\S+:(\\d+)\\s+\\S+\\s+(.*)");

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) {
                        header = false;
                        continue;
                    }
                    Matcher m = pattern.matcher(line);
                    if (m.find()) {
                        String protocol = m.group(1);
                        int port = Integer.parseInt(m.group(2));
                        String processInfo = m.group(3).trim();

                        String processName = "";
                        int pid = 0;
                        // ss process info: users:(("sshd",pid=1234,fd=3))
                        Pattern procPattern = Pattern.compile("\\(\"(.+?)\",pid=(\\d+)");
                        Matcher pm = procPattern.matcher(processInfo);
                        if (pm.find()) {
                            processName = pm.group(1);
                            pid = Integer.parseInt(pm.group(2));
                        }

                        result.add(AssetPortDTO.builder()
                                .protocol(protocol)
                                .localPort(port)
                                .state("LISTEN")
                                .processName(processName.isEmpty() ? null : processName)
                                .processPid(pid > 0 ? pid : null)
                                .build());
                    }
                }
            }
            p.waitFor(15, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.warn("Failed to collect ports on Linux: {}", e.getMessage());
        }

        log.debug("Collected {} listening ports (Linux)", result.size());
        return result;
    }

    /**
     * 通过 PID 解析进程名称（Windows: tasklist, Linux: /proc/pid/cmdline）
     */
    private String resolveProcessName(int pid) {
        if (pid <= 0) return null;
        try {
            if (isWindows) {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c",
                        "tasklist /fi \"PID eq " + pid + "\" /fo csv /nh");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream(), Charset.forName("GBK")))) {
                    String line = reader.readLine();
                    if (line != null && line.contains(",")) {
                        String[] parts = line.split(",");
                        if (parts.length > 0) {
                            String name = parts[0].replace("\"", "").trim();
                            if (!name.equals("信息: 没有运行的任务匹配指定标准。")) {
                                p.waitFor(3, TimeUnit.SECONDS);
                                return name;
                            }
                        }
                    }
                }
                p.waitFor(3, TimeUnit.SECONDS);
            } else {
                java.nio.file.Path cmdline = java.nio.file.Paths.get("/proc/" + pid + "/cmdline");
                if (java.nio.file.Files.exists(cmdline)) {
                    String content = java.nio.file.Files.readString(cmdline);
                    if (!content.isEmpty()) {
                        String exe = content.split("\\0")[0];
                        int slash = exe.lastIndexOf('/');
                        return slash >= 0 ? exe.substring(slash + 1) : exe;
                    }
                }
            }
        } catch (Exception e) {
            // ignore individual process resolution failures
        }
        return null;
    }
}
