package com.secguard.agent.collector.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secguard.common.enums.EventCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多格式日志解析器
 *
 * 支持三种日志格式：
 * 1. syslog - RFC3164 风格（如 /var/log/syslog, auth.log）
 * 2. json   - JSON 结构化日志（如应用输出的 JSON log）
 * 3. plain  - 纯文本（无法识别格式时的兜底）
 *
 * 自动检测格式并解析为结构化字段。
 */
@Component
@Slf4j
public class LogParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * syslog 正则（宽松匹配 RFC3164 风格）
     * 示例: Jan  5 14:23:01 myhost sshd[1234]: Failed password for root
     */
    private static final Pattern SYSLOG_PATTERN = Pattern.compile(
            "^([A-Z][a-z]{2}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+(\\S+)\\s+(\\S+?)(?:\\[(\\d+)])?:\\s+(.+)$"
    );

    /**
     * 解析一行日志
     *
     * @param line   原始日志行
     * @param source 日志来源文件路径
     * @return 解析结果 {format, fields}
     */
    public ParseResult parse(String line, String source) {
        if (line == null || line.isBlank()) {
            return new ParseResult("plain", Map.of("message", ""));
        }

        String trimmed = line.trim();

        // 1. 尝试 JSON 解析
        if (trimmed.startsWith("{")) {
            try {
                return parseJson(trimmed);
            } catch (Exception e) {
                // 不是合法 JSON，继续尝试其他格式
            }
        }

        // 2. 尝试 syslog 解析
        Matcher syslogMatcher = SYSLOG_PATTERN.matcher(trimmed);
        if (syslogMatcher.matches()) {
            return parseSyslog(syslogMatcher);
        }

        // 3. 兜底为 plain 文本
        return parsePlain(trimmed);
    }

    private ParseResult parseSyslog(Matcher m) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("timestamp", m.group(1));
        fields.put("host", m.group(2));
        fields.put("program", m.group(3));
        if (m.group(4) != null) {
            fields.put("pid", m.group(4));
        }
        fields.put("message", m.group(5));
        return new ParseResult("syslog", fields);
    }

    private ParseResult parseJson(String json) throws Exception {
        JsonNode node = mapper.readTree(json);
        Map<String, String> fields = new LinkedHashMap<>();

        node.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (value.isTextual()) {
                fields.put(key, value.asText());
            } else {
                fields.put(key, value.toString());
            }
        });

        return new ParseResult("json", fields);
    }

    private ParseResult parsePlain(String line) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("message", line);
        return new ParseResult("plain", fields);
    }

    /**
     * 根据日志内容推断事件类别
     */
    public EventCategory inferCategory(String line, String source) {
        String lower = line.toLowerCase();
        String srcLower = source != null ? source.toLowerCase() : "";

        // 认证相关
        if (srcLower.contains("auth") || srcLower.contains("secure")
                || lower.contains("password") || lower.contains("login")
                || lower.contains("sshd") || lower.contains("authentication")
                || lower.contains("pam")) {
            return EventCategory.AUTHENTICATION;
        }

        // Web 服务器
        if (srcLower.contains("nginx") || srcLower.contains("apache")
                || srcLower.contains("httpd") || srcLower.contains("access")
                || lower.contains("GET /") || lower.contains("POST /")) {
            return EventCategory.WEB;
        }

        // 防火墙
        if (srcLower.contains("iptables") || srcLower.contains("firewall")
                || lower.contains("DROP") || lower.contains("REJECT")
                || lower.contains("iptables")) {
            return EventCategory.FIREWALL;
        }

        // 系统事件
        if (srcLower.contains("syslog") || srcLower.contains("system")
                || lower.contains("kernel") || lower.contains("systemd")
                || lower.contains("service")) {
            return EventCategory.SYSTEM;
        }

        return EventCategory.SYSTEM;
    }

    /**
     * 解析结果
     */
    public record ParseResult(String format, Map<String, String> fields) {
    }
}
