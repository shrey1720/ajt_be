package com.collaboration.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
public class QuestionController {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public QuestionController(QuestionRepository questionRepository,
                              AnswerRepository answerRepository,
                              UserRepository userRepository,
                              NotificationService notificationService,
                              EmailService emailService) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @PostMapping("/question")
    public ResponseEntity<?> createQuestion(@RequestBody Question question) {
        Question saved = questionRepository.save(question);
        notificationService.publish("question_posted", "New question posted: " + saved.getTitle(),
                Map.of("questionId", saved.getId(), "title", saved.getTitle(), "userId", saved.getUserId()));
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/question/{questionId}/accept/{answerId}")
    public ResponseEntity<?> acceptAnswer(@PathVariable Long questionId, @PathVariable Long answerId) {
        Optional<Question> questionOpt = questionRepository.findById(questionId);
        if (questionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        questionRepository.markAcceptedAnswer(questionId, answerId);
        notificationService.publish("answer_accepted", "An answer was accepted for question: " + questionOpt.get().getTitle(),
                Map.of("questionId", questionId, "answerId", answerId));

        userRepository.findById(questionOpt.get().getUserId()).ifPresent(questionOwner -> {
            String subject = "Answer accepted for your question";
            String body = "<p>Hi " + escapeHtml(questionOwner.getUsername()) + ",</p>"
                    + "<p>Your question \"" + escapeHtml(questionOpt.get().getTitle()) + "\" has an accepted answer.</p>"
                    + "<p><a href=\"" + emailServiceLink(questionId) + "\">View accepted answer</a></p>";
            emailService.sendEmail(questionOwner.getEmail(), subject, body);
        });

        return ResponseEntity.ok(Map.of("message", "Answer accepted", "questionId", questionId, "answerId", answerId));
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

    @GetMapping("/questions")
    public ResponseEntity<?> getAllQuestions(@RequestParam(required = false) String filter) {
        return ResponseEntity.ok(questionRepository.findAll(filter));
    }

    @GetMapping("/question/{id}")
    public ResponseEntity<?> getQuestion(@PathVariable Long id) {
        return questionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/question/{questionId}/award-bounty/{answerId}")
    @Transactional
    public ResponseEntity<?> awardBounty(@PathVariable Long questionId,
                                         @PathVariable Long answerId,
                                         Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }

        Optional<Question> questionOpt = questionRepository.findById(questionId);
        if (questionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Question question = questionOpt.get();

        if (question.getBounty() == null || question.getBounty() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "This question has no bounty to award"));
        }

        Optional<User> callerOpt = userRepository.findByUsername(authentication.getName());
        if (callerOpt.isEmpty() || !callerOpt.get().getId().equals(question.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Only the question owner can award the bounty"));
        }

        Optional<Answer> answerOpt = answerRepository.findById(answerId);
        if (answerOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Answer not found"));
        }
        Answer answer = answerOpt.get();

        if (!answer.getQuestionId().equals(questionId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "This answer does not belong to the specified question"));
        }

        if (answer.getUserId().equals(question.getUserId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot award bounty to yourself"));
        }

        int bountyAmount = question.getBounty();
        userRepository.updateReputation(answer.getUserId(), bountyAmount);
        questionRepository.clearBounty(questionId);

        User caller = callerOpt.get();
        userRepository.findById(answer.getUserId()).ifPresent(recipient -> {
            String subject = "You received a bounty of " + bountyAmount + " reputation points!";
            String body = "<p>Hi " + escapeHtml(recipient.getUsername()) + ",</p>"
                    + "<p>Great news! Your answer on the question \"<strong>" + escapeHtml(question.getTitle()) + "</strong>\" "
                    + "has been awarded a bounty of <strong>" + bountyAmount + " reputation points</strong> by "
                    + escapeHtml(caller.getUsername()) + ".</p>"
                    + "<p>Your reputation has been updated. Keep up the great work!</p>"
                    + "<p><a href=\"" + emailServiceLink(questionId) + "\">View the question</a></p>";
            emailService.sendEmail(recipient.getEmail(), subject, body);
        });

        notificationService.publish("bounty_awarded",
                "Bounty of " + bountyAmount + " reputation points awarded!",
                Map.of("questionId", questionId, "answerId", answerId, "bountyAmount", bountyAmount));

        return ResponseEntity.ok(Map.of(
                "message", "Bounty of " + bountyAmount + " reputation points awarded successfully",
                "questionId", questionId,
                "answerId", answerId,
                "bountyAmount", bountyAmount));
    }
}
