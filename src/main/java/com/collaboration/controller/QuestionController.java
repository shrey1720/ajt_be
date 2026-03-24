package com.collaboration.controller;

import com.collaboration.model.Question;
import com.collaboration.repository.QuestionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class QuestionController {

    private final QuestionRepository questionRepository;

    public QuestionController(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @PostMapping("/question")
    public ResponseEntity<?> createQuestion(@RequestBody Question question) {
        Question saved = questionRepository.save(question);
        return ResponseEntity.ok(saved);
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
}
