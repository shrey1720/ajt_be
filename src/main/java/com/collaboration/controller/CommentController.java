package com.collaboration.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.collaboration.model.Comment;
import com.collaboration.repository.AnswerRepository;
import com.collaboration.repository.CommentRepository;
import com.collaboration.repository.QuestionRepository;
import com.collaboration.repository.UserRepository;
import com.collaboration.service.EmailService;
import com.collaboration.service.NotificationService;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentRepository commentRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public CommentController(CommentRepository commentRepository,
                             QuestionRepository questionRepository,
                             AnswerRepository answerRepository,
                             UserRepository userRepository,
                             NotificationService notificationService,
                             EmailService emailService) {
        this.commentRepository = commentRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @PostMapping("/comment")
    public ResponseEntity<?> createComment(@RequestBody Comment comment) {
        Comment saved = commentRepository.save(comment);
        Long parentId = saved.getAnswerId() != null ? saved.getAnswerId() : saved.getQuestionId();
        String parentType = saved.getAnswerId() != null ? "answer" : "question";

        notificationService.publish("comment_posted", "New comment posted", Map.of(
                "parentType", parentType,
                "parentId", parentId,
                "userId", saved.getUserId()));

        if (saved.getAnswerId() != null) {
            answerRepository.findById(saved.getAnswerId()).ifPresent(answer -> {
                if (!answer.getUserId().equals(saved.getUserId())) {
                    userRepository.findById(answer.getUserId()).ifPresent(recipient -> {
                        String bodyQuestionLink = emailServiceLink(answer.getQuestionId());
                        String subject = "New comment on your answer";
                        String body = "<p>Hi " + escapeHtml(recipient.getUsername()) + ",</p>"
                                + "<p>Your answer on the question \"" + escapeHtml(answer.getQuestionId().toString()) + "\" received a new comment.</p>"
                                + "<p>Comment:</p>"
                                + "<blockquote>" + escapeHtml(saved.getCommentText()) + "</blockquote>"
                                + "<p><a href=\"" + bodyQuestionLink + "\">View the discussion</a></p>";
                        emailService.sendEmail(recipient.getEmail(), subject, body);
                    });
                }
            });
        } else {
            questionRepository.findById(saved.getQuestionId()).ifPresent(question -> {
                if (!question.getUserId().equals(saved.getUserId())) {
                    userRepository.findById(question.getUserId()).ifPresent(recipient -> {
                        String subject = "New comment on your question";
                        String body = "<p>Hi " + escapeHtml(recipient.getUsername()) + ",</p>"
                                + "<p>Your question \"" + escapeHtml(question.getTitle()) + "\" received a new comment.</p>"
                                + "<p>Comment:</p>"
                                + "<blockquote>" + escapeHtml(saved.getCommentText()) + "</blockquote>"
                                + "<p><a href=\"" + emailServiceLink(question.getId()) + "\">View the comment</a></p>";
                        emailService.sendEmail(recipient.getEmail(), subject, body);
                    });
                }
            });
        }

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/comments/{id}")
    public ResponseEntity<?> getComments(@PathVariable Long id, @RequestParam String type) {
        if ("question".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(commentRepository.findByQuestionId(id));
        } else {
            return ResponseEntity.ok(commentRepository.findByAnswerId(id));
        }
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

    private String emailServiceLink(Long questionId) {
        return System.getProperty("app.base.url", "http://localhost:8080") + "/question/" + questionId;
    }
}
