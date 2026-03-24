package com.collaboration.controller;

import com.collaboration.model.Vote;
import com.collaboration.repository.AnswerRepository;
import com.collaboration.repository.QuestionRepository;
import com.collaboration.repository.VoteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class VoteController {

    private final VoteRepository voteRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    public VoteController(VoteRepository voteRepository, QuestionRepository questionRepository, AnswerRepository answerRepository) {
        this.voteRepository = voteRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    @PostMapping("/vote")
    @Transactional
    public ResponseEntity<?> castVote(@RequestBody Vote vote) {
        if (vote.getVoteType() != 1 && vote.getVoteType() != -1) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid vote type"));
        }

        if (vote.getQuestionId() != null) {
            Optional<Vote> existing = voteRepository.findByUserAndQuestion(vote.getUserId(), vote.getQuestionId());
            if (existing.isPresent()) {
                Vote prev = existing.get();
                if (prev.getVoteType().equals(vote.getVoteType())) {
                    voteRepository.delete(prev.getId());
                    questionRepository.updateVotes(vote.getQuestionId(), -vote.getVoteType());
                    return ResponseEntity.ok(Map.of("message", "Vote removed"));
                } else {
                    voteRepository.delete(prev.getId());
                    questionRepository.updateVotes(vote.getQuestionId(), -prev.getVoteType());
                    
                    voteRepository.save(vote);
                    questionRepository.updateVotes(vote.getQuestionId(), vote.getVoteType());
                    return ResponseEntity.ok(Map.of("message", "Vote changed"));
                }
            } else {
                voteRepository.save(vote);
                questionRepository.updateVotes(vote.getQuestionId(), vote.getVoteType());
                return ResponseEntity.ok(Map.of("message", "Vote recorded"));
            }
        } else if (vote.getAnswerId() != null) {
            Optional<Vote> existing = voteRepository.findByUserAndAnswer(vote.getUserId(), vote.getAnswerId());
            if (existing.isPresent()) {
                Vote prev = existing.get();
                if (prev.getVoteType().equals(vote.getVoteType())) {
                    voteRepository.delete(prev.getId());
                    answerRepository.updateVotes(vote.getAnswerId(), -vote.getVoteType());
                    return ResponseEntity.ok(Map.of("message", "Vote removed"));
                } else {
                    voteRepository.delete(prev.getId());
                    answerRepository.updateVotes(vote.getAnswerId(), -prev.getVoteType());
                    
                    voteRepository.save(vote);
                    answerRepository.updateVotes(vote.getAnswerId(), vote.getVoteType());
                    return ResponseEntity.ok(Map.of("message", "Vote changed"));
                }
            } else {
                voteRepository.save(vote);
                answerRepository.updateVotes(vote.getAnswerId(), vote.getVoteType());
                return ResponseEntity.ok(Map.of("message", "Vote recorded"));
            }
        }
        
        return ResponseEntity.badRequest().body(Map.of("error", "Must provide questionId or answerId"));
    }
}
