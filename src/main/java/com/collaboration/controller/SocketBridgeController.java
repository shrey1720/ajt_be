package com.collaboration.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.collaboration.repository.UserRepository;
import com.collaboration.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class SocketBridgeController {

    private static final Logger log = LoggerFactory.getLogger(SocketBridgeController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;

    public SocketBridgeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/extension/ask")
    public ResponseEntity<?> bridgeToSocket(@RequestBody Map<String, Object> payload) {
        try (Socket socket = new Socket("127.0.0.1", 9090);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
             
             Map<String, Object> bridgePayload = new HashMap<>(payload);
             bridgePayload.putIfAbsent("type", "QUESTION");
             
             // If username is provided, resolve to userId automatically
             if (payload.containsKey("username")) {
                String username = (String) payload.get("username");
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    bridgePayload.put("userId", userOpt.get().getId());
                    log.info("Resolved username '{}' to userId {}", username, userOpt.get().getId());
                } else {
                    log.warn("Username '{}' not found, defaulting to fallback userId 1 (admin)", username);
                    bridgePayload.put("userId", 1L); 
                }
             } else {
                bridgePayload.putIfAbsent("userId", 1L);
             }
             
             String json = objectMapper.writeValueAsString(bridgePayload);
             log.info("Bridging final payload to Socket Server: {}", json);
             
             out.println(json);
             
             String responseLine = in.readLine();
             if (responseLine != null) {
                return ResponseEntity.ok(objectMapper.readTree(responseLine));
             }

        } catch (Exception e) {
            log.error("Bridge to socket server failed", e);
            return ResponseEntity.status(500).body(Map.of("error", "Socket bridging failed: " + e.getMessage()));
        }
        
        return ResponseEntity.badRequest().body(Map.of("error", "No response from socket"));
    }
}
