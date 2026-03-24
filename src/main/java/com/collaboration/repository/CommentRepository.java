package com.collaboration.repository;

import com.collaboration.model.Comment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CommentRepository {
    private final JdbcTemplate jdbcTemplate;

    public CommentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Comment> commentRowMapper = (rs, rowNum) -> {
        Comment c = new Comment();
        c.setId(rs.getLong("id"));
        c.setUserId(rs.getLong("user_id"));
        c.setQuestionId(rs.getObject("question_id", Long.class));
        c.setAnswerId(rs.getObject("answer_id", Long.class));
        c.setCommentText(rs.getString("comment_text"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUsername(rs.getString("username"));
        return c;
    };

    public Comment save(Comment comment) {
        String sql = "INSERT INTO comments (user_id, question_id, answer_id, comment_text) VALUES (?, ?, ?, ?) RETURNING id";
        Long id = jdbcTemplate.queryForObject(sql, Long.class,
                comment.getUserId(), comment.getQuestionId(), comment.getAnswerId(), comment.getCommentText());
        comment.setId(id);
        return comment;
    }

    public List<Comment> findByQuestionId(Long questionId) {
        String sql = "SELECT c.*, u.username FROM comments c JOIN users u ON c.user_id = u.id WHERE c.question_id = ? ORDER BY c.created_at ASC";
        return jdbcTemplate.query(sql, commentRowMapper, questionId);
    }

    public List<Comment> findByAnswerId(Long answerId) {
        String sql = "SELECT c.*, u.username FROM comments c JOIN users u ON c.user_id = u.id WHERE c.answer_id = ? ORDER BY c.created_at ASC";
        return jdbcTemplate.query(sql, commentRowMapper, answerId);
    }
}
