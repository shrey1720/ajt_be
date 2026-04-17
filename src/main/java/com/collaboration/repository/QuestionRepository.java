package com.collaboration.repository;

import com.collaboration.model.Question;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class QuestionRepository {
    private final JdbcTemplate jdbcTemplate;

    public QuestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Question> questionRowMapper = (rs, rowNum) -> {
        Question q = new Question();
        q.setId(rs.getLong("id"));
        q.setUserId(rs.getLong("user_id"));
        q.setTitle(rs.getString("title"));
        q.setDescription(rs.getString("description"));
        q.setCode(rs.getString("code"));
        q.setTags(rs.getString("tags"));
        q.setVotes(rs.getInt("votes"));
        q.setBounty(rs.getInt("bounty"));
        q.setStatus(rs.getString("status"));
        q.setAcceptedAnswerId(rs.getLong("accepted_answer_id"));
        if (rs.wasNull()) {
            q.setAcceptedAnswerId(null);
        }
        q.setCreatedAt(rs.getTimestamp("created_at"));
        q.setUsername(rs.getString("username"));
        return q;
    };

    public Question save(Question question) {
        String sql = "INSERT INTO questions (user_id, title, description, code, tags, bounty) VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        Long id = jdbcTemplate.queryForObject(sql, Long.class,
                question.getUserId(), question.getTitle(), question.getDescription(), question.getCode(), question.getTags(), question.getBounty());
        question.setId(id);
        return question;
    }

    public List<Question> findAll(String filter) {
        String sql = "SELECT q.*, u.username FROM questions q JOIN users u ON q.user_id = u.id ";
        String orderBy = " ORDER BY q.created_at DESC";
        
        if ("hot".equalsIgnoreCase(filter)) {
            orderBy = " ORDER BY q.votes DESC, q.created_at DESC";
        } else if ("week".equalsIgnoreCase(filter)) {
            sql += " WHERE q.created_at >= CURRENT_DATE - INTERVAL '7 days'";
        } else if ("month".equalsIgnoreCase(filter)) {
            sql += " WHERE q.created_at >= CURRENT_DATE - INTERVAL '30 days'";
        } else if ("bounty".equalsIgnoreCase(filter)) {
            sql += " WHERE q.bounty > 0";
            orderBy = " ORDER BY q.bounty DESC, q.created_at DESC";
        }
        
        return jdbcTemplate.query(sql + orderBy, questionRowMapper);
    }

    public Optional<Question> findById(Long id) {
        String sql = "SELECT q.*, u.username FROM questions q JOIN users u ON q.user_id = u.id WHERE q.id = ?";
        List<Question> result = jdbcTemplate.query(sql, questionRowMapper, id);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public void updateVotes(Long questionId, int increment) {
        jdbcTemplate.update("UPDATE questions SET votes = votes + ? WHERE id = ?", increment, questionId);
    }

    public void markAcceptedAnswer(Long questionId, Long answerId) {
        jdbcTemplate.update("UPDATE questions SET accepted_answer_id = ? WHERE id = ?", answerId, questionId);
    }
}
