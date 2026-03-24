package com.collaboration.controller;

import com.collaboration.model.Answer;
import com.collaboration.repository.AnswerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AnswerController {

    private final AnswerRepository answerRepository;

    public AnswerController(AnswerRepository answerRepository) {
        this.answerRepository = answerRepository;
    }

    @PostMapping("/answer")
    public ResponseEntity<?> createAnswer(@RequestBody Answer answer) {
        Answer saved = answerRepository.save(answer);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/answers/{questionId}")
    public ResponseEntity<?> getAnswersByQuestionId(@PathVariable Long questionId) {
        return ResponseEntity.ok(answerRepository.findByQuestionId(questionId));
    }
}
