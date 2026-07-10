package com.secguard.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secguard.server.entity.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警 WebSocket 处理器
 *
 * 端点：/ws/alerts
 * 功能：实时推送新告警到已连接的 Dashboard 客户端
 */
@Component
@Slf4j
public class AlertWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    public AlertWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket client connected: {} (total: {})", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket client disconnected: {} (total: {})", session.getId(), sessions.size());
    }

    /**
     * 广播告警到所有已连接客户端
     */
    public void broadcastAlert(Alert alert) {
        if (sessions.isEmpty()) return;

        try {
            Map<String, Object> message = Map.of(
                    "type", "new_alert",
                    "data", Map.of(
                            "id", alert.getId(),
                            "ruleId", alert.getRuleId() != null ? alert.getRuleId() : 0,
                            "ruleName", alert.getRuleName() != null ? alert.getRuleName() : "",
                            "severity", alert.getSeverity().name(),
                            "category", alert.getCategory().name(),
                            "description", alert.getDescription() != null ? alert.getDescription() : "",
                            "sourceIp", alert.getSourceIp() != null ? alert.getSourceIp() : "",
                            "status", alert.getStatus().name(),
                            "createdAt", alert.getCreatedAt().toString()
                    )
            );

            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("Failed to send alert to session {}: {}", session.getId(), e.getMessage());
                    }
                }
            }

            log.debug("Alert broadcast to {} client(s)", sessions.size());
        } catch (Exception e) {
            log.error("Failed to broadcast alert: {}", e.getMessage());
        }
    }

    /**
     * 当前连接的客户端数量
     */
    public int getClientCount() {
        return sessions.size();
    }
}
