package com.collaboration.controller;

import com.collaboration.model.Answer;
import com.collaboration.model.Question;
import com.collaboration.model.User;
import com.collaboration.model.Vote;
import com.collaboration.repository.AnswerRepository;
import com.collaboration.repository.QuestionRepository;
import com.collaboration.repository.UserRepository;
import com.collaboration.repository.VoteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class VoteController {

    private final VoteRepository voteRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;

    public VoteController(VoteRepository voteRepository, QuestionRepository questionRepository,
                          AnswerRepository answerRepository, UserRepository userRepository) {
        this.voteRepository = voteRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/votes/me")
    public ResponseEntity<?> myVotes(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            return ResponseEntity.ok(Map.of(
                    "questionUpvotes", List.of(),
                    "answerUpvotes", List.of()
            ));
        }
        Optional<User> userOpt = userRepository.findByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "questionUpvotes", List.of(),
                    "answerUpvotes", List.of()
            ));
        }
        Long uid = userOpt.get().getId();
        List<Long> qids = voteRepository.findQuestionIdsUpvotedByUser(uid);
        List<Long> aids = voteRepository.findAnswerIdsUpvotedByUser(uid);
        Map<String, Object> body = new HashMap<>();
        body.put("questionUpvotes", qids);
        body.put("answerUpvotes", aids);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/vote")
    @Transactional
    public ResponseEntity<?> castVote(@RequestBody Vote vote) {
        if (vote.getVoteType() != 1 && vote.getVoteType() != -1) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid vote type"));
        }

        if (vote.getQuestionId() != null) {
            if (vote.getVoteType() != 1) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only upvotes are allowed on questions"));
            }
            return ResponseEntity.ok(toggleQuestionUpvote(vote));
        }

        if (vote.getAnswerId() != null) {
            if (vote.getVoteType() != 1) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only upvotes are allowed on answers"));
            }
            return ResponseEntity.ok(toggleAnswerUpvote(vote));
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Must provide questionId or answerId"));
    }

    /** First click: add +1. Second click (same user): remove vote. Only upvotes; legacy downvotes are cleared on first up. */
    private Map<String, Object> toggleQuestionUpvote(Vote vote) {
        Long qid = vote.getQuestionId();
        Map<String, Object> body = new HashMap<>();
        Optional<Vote> existing = voteRepository.findByUserAndQuestion(vote.getUserId(), qid);
        if (existing.isPresent()) {
            Vote prev = existing.get();
            if (prev.getVoteType() == 1) {
                voteRepository.delete(prev.getId());
                questionRepository.updateVotes(qid, -1);
                body.put("message", "Vote removed");
                body.put("upvoted", false);
                body.put("votes", questionVoteTotal(qid));
                return body;
            }
            voteRepository.delete(prev.getId());
            questionRepository.updateVotes(qid, -prev.getVoteType());
        }
        voteRepository.save(vote);
        questionRepository.updateVotes(qid, 1);
        body.put("message", "Vote recorded");
        body.put("upvoted", true);
        body.put("votes", questionVoteTotal(qid));
        return body;
    }

    private Map<String, Object> toggleAnswerUpvote(Vote vote) {
        Long aid = vote.getAnswerId();
        Map<String, Object> body = new HashMap<>();
        Optional<Vote> existing = voteRepository.findByUserAndAnswer(vote.getUserId(), aid);
        if (existing.isPresent()) {
            Vote prev = existing.get();
            if (prev.getVoteType() == 1) {
                voteRepository.delete(prev.getId());
                answerRepository.updateVotes(aid, -1);
                body.put("message", "Vote removed");
                body.put("upvoted", false);
                body.put("votes", answerVoteTotal(aid));
                return body;
            }
            voteRepository.delete(prev.getId());
            answerRepository.updateVotes(aid, -prev.getVoteType());
        }
        voteRepository.save(vote);
        answerRepository.updateVotes(aid, 1);
        body.put("message", "Vote recorded");
        body.put("upvoted", true);
        body.put("votes", answerVoteTotal(aid));
        return body;
    }

    private int questionVoteTotal(Long questionId) {
        Optional<Question> q = questionRepository.findById(questionId);
        if (q.isEmpty()) {
            return 0;
        }
        Integer v = q.get().getVotes();
        return v != null ? v : 0;
    }

    private int answerVoteTotal(Long answerId) {
        return answerRepository.findById(answerId)
                .map(a -> a.getVotes() != null ? a.getVotes() : 0)
                .orElse(0);
    }
}
