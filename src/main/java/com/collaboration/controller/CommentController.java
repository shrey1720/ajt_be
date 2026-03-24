package com.collaboration.controller;

import com.collaboration.model.Comment;
import com.collaboration.repository.CommentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentRepository commentRepository;

    public CommentController(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @PostMapping("/comment")
    public ResponseEntity<?> createComment(@RequestBody Comment comment) {
        Comment saved = commentRepository.save(comment);
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
}
