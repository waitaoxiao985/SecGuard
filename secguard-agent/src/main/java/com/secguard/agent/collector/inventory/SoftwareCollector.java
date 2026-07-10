package com.secguard.agent.collector.inventory;

import com.secguard.common.dto.AssetSoftwareDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 已安装软件采集器
 *
 * Windows: 通过 PowerShell 读取注册表 Uninstall 项
 * Linux: 解析 dpkg 或 rpm 输出
 */
@Component
@Slf4j
public class SoftwareCollector {

    private final boolean isWindows;

    public SoftwareCollector() {
        this.isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public List<AssetSoftwareDTO> collect() {
        if (isWindows) {
            return collectWindows();
        } else {
            return collectLinux();
        }
    }

    private List<AssetSoftwareDTO> collectWindows() {
        List<AssetSoftwareDTO> result = new ArrayList<>();
        try {
            // 使用 PowerShell 从注册表读取已安装软件
            String psCmd = String.join(" ",
                    "Get-ItemProperty HKLM:\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\* |",
                    "Select-Object DisplayName, DisplayVersion, Publisher, InstallSource |",
                    "Where-Object { $_.DisplayName } |",
                    "ConvertTo-Csv -NoTypeInformation");

            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-Command", psCmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), Charset.forName("GBK")))) {
                String line;
                boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) {
                        header = false;
                        continue;
                    }
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("\"")) {
                        // CSV: "Name","Version","Publisher","InstallSource"
                        String[] parts = parseCsvLine(line);
                        if (parts.length >= 1 && !parts[0].isEmpty()) {
                            result.add(AssetSoftwareDTO.builder()
                                    .name(parts[0])
                                    .version(parts.length > 1 ? parts[1] : null)
                                    .vendor(parts.length > 2 ? parts[2] : null)
                                    .format("registry")
                                    .build());
                        }
                    }
                }
            }
            p.waitFor(30, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.warn("Failed to collect Windows software list: {}", e.getMessage());
        }

        log.debug("Collected {} software entries (Windows registry)", result.size());
        return result;
    }

    private List<AssetSoftwareDTO> collectLinux() {
        List<AssetSoftwareDTO> result = new ArrayList<>();

        // 尝试 dpkg (Debian/Ubuntu)
        if (Files.exists(Paths.get("/usr/bin/dpkg"))) {
            result.addAll(collectDpkg());
        }
        // 尝试 rpm (CentOS/RHEL/Fedora)
        else if (Files.exists(Paths.get("/usr/bin/rpm"))) {
            result.addAll(collectRpm());
        }

        log.debug("Collected {} software entries (Linux)", result.size());
        return result;
    }

    private List<AssetSoftwareDTO> collectDpkg() {
        List<AssetSoftwareDTO> result = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("dpkg-query", "-W", "-f",
                    "${Package}\\t${Version}\\t${Maintainer}\\n");
            pb.redirectErrorStream(true);
            Process p = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\t", -1);
                    if (parts.length >= 1 && !parts[0].isEmpty()) {
                        result.add(AssetSoftwareDTO.builder()
                                .name(parts[0])
                                .version(parts.length > 1 ? parts[1] : null)
                                .vendor(parts.length > 2 ? parts[2] : null)
                                .format("deb")
                                .build());
                    }
                }
            }
            p.waitFor(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("dpkg-query failed: {}", e.getMessage());
        }
        return result;
    }

    private List<AssetSoftwareDTO> collectRpm() {
        List<AssetSoftwareDTO> result = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("rpm", "-qa", "--queryformat",
                    "%{NAME}\\t%{VERSION}-%{RELEASE}\\t%{VENDOR}\\n");
            pb.redirectErrorStream(true);
            Process p = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\t", -1);
                    if (parts.length >= 1 && !parts[0].isEmpty()) {
                        result.add(AssetSoftwareDTO.builder()
                                .name(parts[0])
                                .version(parts.length > 1 ? parts[1] : null)
                                .vendor(parts.length > 2 ? parts[2] : null)
                                .format("rpm")
                                .build());
                    }
                }
            }
            p.waitFor(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("rpm query failed: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 简易 CSV 行解析（处理引号包裹的字段）
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields.toArray(new String[0]);
    }
}
