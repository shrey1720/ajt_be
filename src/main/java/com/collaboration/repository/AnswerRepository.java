package com.collaboration.repository;

import com.collaboration.model.Answer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AnswerRepository {
    private final JdbcTemplate jdbcTemplate;

    public AnswerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Answer> answerRowMapper = (rs, rowNum) -> {
        Answer a = new Answer();
        a.setId(rs.getLong("id"));
        a.setQuestionId(rs.getLong("question_id"));
        a.setUserId(rs.getLong("user_id"));
        a.setAnswerText(rs.getString("answer_text"));
        a.setVotes(rs.getInt("votes"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        a.setUsername(rs.getString("username"));
        return a;
    };

    public Answer save(Answer answer) {
        String sql = "INSERT INTO answers (question_id, user_id, answer_text) VALUES (?, ?, ?) RETURNING id";
        Long id = jdbcTemplate.queryForObject(sql, Long.class,
                answer.getQuestionId(), answer.getUserId(), answer.getAnswerText());
        answer.setId(id);
        return answer;
    }

    public List<Answer> findByQuestionId(Long questionId) {
        String sql = "SELECT a.*, u.username FROM answers a JOIN users u ON a.user_id = u.id WHERE a.question_id = ? ORDER BY a.votes DESC, a.created_at ASC";
        return jdbcTemplate.query(sql, answerRowMapper, questionId);
    }
    
    public Optional<Answer> findById(Long id) {
        String sql = "SELECT a.*, u.username FROM answers a JOIN users u ON a.user_id = u.id WHERE a.id = ?";
        List<Answer> result = jdbcTemplate.query(sql, answerRowMapper, id);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public void updateVotes(Long answerId, int increment) {
        jdbcTemplate.update("UPDATE answers SET votes = votes + ? WHERE id = ?", increment, answerId);
    }
}
