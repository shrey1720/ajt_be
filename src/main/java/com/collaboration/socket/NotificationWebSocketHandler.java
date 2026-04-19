package com.collaboration.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Maps Session ID to User ID (optional, for targeted notifications)
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: session={}", session.getId());
        sessions.put(session.getId(), session);
        
        // Send initial connection success message
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
            "type", "connection_established",
            "message", "Linked to CodeCollab Notification Bridge"
        ))));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: session={}, status={}", session.getId(), status);
        sessions.remove(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload)) {
            session.sendMessage(new TextMessage("pong"));
            return;
        }
        log.debug("Received WebSocket message: {}", payload);
    }

    /**
     * Broadcasts a notification to all connected clients (Browsers and VS Code)
     */
    public void broadcast(Map<String, Object> notification) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(notification);
        } catch (Exception e) {
            log.error("Failed to serialize notification", e);
            return;
        }

        TextMessage textMessage = new TextMessage(payload);
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.warn("Failed to send socket message to session {}: {}", session.getId(), e.getMessage());
                }
            }
        });
    }
}
