package com.collaboration.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.collaboration.model.Answer;
import com.collaboration.model.Question;
import com.collaboration.model.User;
import com.collaboration.repository.AnswerRepository;
import com.collaboration.repository.QuestionRepository;
import com.collaboration.repository.UserRepository;
import com.collaboration.service.EmailService;
import com.collaboration.service.NotificationService;

@RestController
@RequestMapping("/api")
public class AnswerController {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public AnswerController(AnswerRepository answerRepository,
                            QuestionRepository questionRepository,
                            UserRepository userRepository,
                            NotificationService notificationService,
                            EmailService emailService) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @PostMapping("/answer")
    public ResponseEntity<?> createAnswer(@RequestBody Answer answer) {
        Answer saved = answerRepository.save(answer);
        notificationService.publish("answer_posted", "New answer posted", Map.of(
                "questionId", saved.getQuestionId(),
                "answerId", saved.getId(),
                "userId", saved.getUserId()));

        questionRepository.findById(saved.getQuestionId()).ifPresent(question -> {
            if (!question.getUserId().equals(saved.getUserId())) {
                userRepository.findById(question.getUserId()).ifPresent(questionOwner -> {
                    String subject = "New answer posted on your question";
                    String body = "<p>Hi " + escapeHtml(questionOwner.getUsername()) + ",</p>"
                            + "<p>Your question \"" + escapeHtml(question.getTitle()) + "\" has a new answer.</p>"
                            + "<p>Answer preview:</p>"
                            + "<blockquote>" + escapeHtml(saved.getAnswerText()) + "</blockquote>"
                            + "<p><a href=\"" + envBaseUrl() + "/question/" + question.getId() + "\">View the answer</a></p>";
                    emailService.sendEmail(questionOwner.getEmail(), subject, body);
                });
            }
        });

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/answers/{questionId}")
    public ResponseEntity<?> getAnswersByQuestionId(@PathVariable Long questionId) {
        return ResponseEntity.ok(answerRepository.findByQuestionId(questionId));
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private String envBaseUrl() {
        return System.getProperty("app.base.url", "http://localhost:8080");
    }
}
