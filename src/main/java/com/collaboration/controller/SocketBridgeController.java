package com.collaboration.controller;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collaboration.model.Question;
import com.collaboration.model.User;
import com.collaboration.repository.QuestionRepository;
import com.collaboration.repository.UserRepository;
import com.collaboration.service.NotificationService;

/**
 * HTTP bridge for the VS Code extension.
 * Accepts a POST at /api/extension/ask and saves the question directly to
 * the database — no internal socket hop needed, fully compatible with Render.
 */
@RestController
@RequestMapping("/api")
public class SocketBridgeController {

    private static final Logger log = LoggerFactory.getLogger(SocketBridgeController.class);

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public SocketBridgeController(QuestionRepository questionRepository, UserRepository userRepository,
                                  NotificationService notificationService) {
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @PostMapping("/extension/ask")
    public ResponseEntity<?> receiveFromExtension(@RequestBody Map<String, Object> payload) {
        try {
            log.info("Received question from VS Code extension: {}", payload);

            Long userId = 1L;
            if (payload.containsKey("username")) {
                String username = payload.get("username").toString();
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    userId = userOpt.get().getId();
                    log.info("Resolved username '{}' to userId {}", username, userId);
                } else {
                    log.warn("Username '{}' not found, using default userId 1 (admin)", username);
                }
            } else if (payload.containsKey("userId")) {
                userId = Long.valueOf(payload.get("userId").toString());
            }

            Question q = new Question();
            q.setUserId(userId);
            q.setTitle(payload.getOrDefault("title", "Untitled Question from VS Code").toString());
            q.setDescription(payload.getOrDefault("description", "Posted via VS Code Code Collab extension").toString());
            q.setCode(payload.getOrDefault("code", "").toString());
            q.setTags(payload.getOrDefault("tags", "vscode").toString());

            Question saved = questionRepository.save(q);
            log.info("Question saved successfully with id={}", saved.getId());
            notificationService.publish("question_posted", "New question posted from VS Code extension", Map.of(
                    "questionId", saved.getId(),
                    "title", saved.getTitle(),
                    "userId", saved.getUserId()));

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "questionId", saved.getId(),
                "message", "Your question has been posted to the collaboration engine!"
            ));

        } catch (Exception e) {
            log.error("Error saving question from VS Code extension", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "error", "Failed to save question: " + e.getMessage()
            ));
        }
    }
}
