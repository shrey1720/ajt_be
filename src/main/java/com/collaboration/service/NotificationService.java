package com.collaboration.service;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.collaboration.socket.NotificationWebSocketHandler;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final NotificationWebSocketHandler webSocketHandler;

    public NotificationService(NotificationWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("message", "connected", "timestamp", Instant.now().toString())));
        } catch (IOException ex) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void publish(String type, String message, Map<String, Object> details) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("message", message);
        event.put("timestamp", Instant.now().toString());
        if (details != null) {
            event.put("details", details);
        }

        // 1. Broadcast to SSE (Browsers)
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(event));
            } catch (IOException e) {
                log.warn("Removing SSE emitter because of error", e);
                emitters.remove(emitter);
            }
        });

        // 2. Broadcast to WebSockets (VS Code & Modern browsers)
        webSocketHandler.broadcast(event);
    }
}
