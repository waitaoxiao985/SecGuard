package com.secguard.agent.collector.inventory;

import com.secguard.common.dto.AssetNetworkDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 网络接口采集器
 *
 * 使用 Java NetworkInterface API 采集网卡信息，
 * 通过 OS 命令补充网关和 DNS 信息。
 */
@Component
@Slf4j
public class NetworkCollector {

    private final boolean isWindows;

    public NetworkCollector() {
        this.isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public List<AssetNetworkDTO> collect() {
        List<AssetNetworkDTO> result = new ArrayList<>();
        String gateway = getDefaultGateway();
        String dns = getDnsServer();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) return result;

            for (NetworkInterface ni : Collections.list(interfaces)) {
                // 跳过回环和未启用的接口
                if (ni.isLoopback() || !ni.isUp()) continue;

                String name = ni.getDisplayName();
                String mac = formatMac(ni.getHardwareAddress());

                String ipv4 = null;
                String ipv6 = null;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                for (InetAddress addr : Collections.list(addresses)) {
                    if (addr instanceof Inet4Address) {
                        ipv4 = addr.getHostAddress();
                    } else if (addr instanceof Inet6Address && !addr.isLinkLocalAddress()) {
                        ipv6 = addr.getHostAddress();
                    }
                }

                // 至少要有 IPv4 才记录
                if (ipv4 == null && ipv6 == null) continue;

                result.add(AssetNetworkDTO.builder()
                        .interfaceName(name)
                        .macAddress(mac)
                        .ipv4(ipv4)
                        .ipv6(ipv6)
                        .gateway(gateway)
                        .dns(dns)
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to collect network interfaces: {}", e.getMessage());
        }

        log.debug("Collected {} network interfaces", result.size());
        return result;
    }

    private String formatMac(byte[] hw) {
        if (hw == null || hw.length == 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hw.length; i++) {
            sb.append(String.format("%02X", hw[i]));
            if (i < hw.length - 1) sb.append(":");
        }
        return sb.toString();
    }

    private String getDefaultGateway() {
        try {
            if (isWindows) {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c",
                        "route print 0.0.0.0");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream(), Charset.forName("GBK")))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        // 匹配: 0.0.0.0  0.0.0.0  <gateway>  <interface>  <metric>
                        if (line.startsWith("0.0.0.0")) {
                            String[] parts = line.split("\\s+");
                            if (parts.length >= 3) {
                                p.waitFor(5, TimeUnit.SECONDS);
                                return parts[2];
                            }
                        }
                    }
                }
                p.waitFor(5, TimeUnit.SECONDS);
            } else {
                ProcessBuilder pb = new ProcessBuilder("ip", "route", "show", "default");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && line.contains("via")) {
                        String[] parts = line.split("\\s+");
                        for (int i = 0; i < parts.length; i++) {
                            if ("via".equals(parts[i]) && i + 1 < parts.length) {
                                p.waitFor(5, TimeUnit.SECONDS);
                                return parts[i + 1];
                            }
                        }
                    }
                }
                p.waitFor(5, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.debug("Failed to get default gateway: {}", e.getMessage());
        }
        return null;
    }

    private String getDnsServer() {
        try {
            if (isWindows) {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c",
                        "powershell -NoProfile -Command Get-DnsClientServerAddress -AddressFamily IPv4 | Select-Object -ExpandProperty ServerAddresses");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream(), Charset.forName("GBK")))) {
                    String line;
                    List<String> dnsList = new ArrayList<>();
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && line.contains(".")) {
                            dnsList.add(line);
                        }
                    }
                    p.waitFor(5, TimeUnit.SECONDS);
                    return dnsList.isEmpty() ? null : String.join(", ", dnsList);
                }
            } else {
                java.nio.file.Path resolvConf = java.nio.file.Paths.get("/etc/resolv.conf");
                if (java.nio.file.Files.exists(resolvConf)) {
                    List<String> dnsList = new ArrayList<>();
                    for (String line : java.nio.file.Files.readAllLines(resolvConf)) {
                        if (line.startsWith("nameserver")) {
                            String[] parts = line.split("\\s+");
                            if (parts.length >= 2) {
                                dnsList.add(parts[1]);
                            }
                        }
                    }
                    return dnsList.isEmpty() ? null : String.join(", ", dnsList);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get DNS servers: {}", e.getMessage());
        }
        return null;
    }
}
