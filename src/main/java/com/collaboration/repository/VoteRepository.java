package com.collaboration.repository;

import com.collaboration.model.Vote;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class VoteRepository {
    private final JdbcTemplate jdbcTemplate;

    public VoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Vote> voteRowMapper = (rs, rowNum) -> {
        Vote v = new Vote();
        v.setId(rs.getLong("id"));
        v.setUserId(rs.getLong("user_id"));
        v.setQuestionId(getNullableLong(rs.getObject("question_id")));
        v.setAnswerId(getNullableLong(rs.getObject("answer_id")));
        v.setVoteType(rs.getInt("vote_type"));
        v.setCreatedAt(rs.getTimestamp("created_at"));
        return v;
    };

    private Long getNullableLong(Object dbValue) {
        if (dbValue == null) {
            return null;
        }
        if (dbValue instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("Expected numeric vote id value but got: " + dbValue.getClass().getName());
    }

    public Vote save(Vote vote) {
        String sql = "INSERT INTO votes (user_id, question_id, answer_id, vote_type) VALUES (?, ?, ?, ?) RETURNING id";
        Long id = jdbcTemplate.queryForObject(sql, Long.class,
                vote.getUserId(), vote.getQuestionId(), vote.getAnswerId(), vote.getVoteType());
        vote.setId(id);
        return vote;
    }
    
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM votes WHERE id = ?", id);
    }

    public Optional<Vote> findByUserAndQuestion(Long userId, Long questionId) {
        String sql = "SELECT * FROM votes WHERE user_id = ? AND question_id = ?";
        List<Vote> result = jdbcTemplate.query(sql, voteRowMapper, userId, questionId);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public Optional<Vote> findByUserAndAnswer(Long userId, Long answerId) {
        String sql = "SELECT * FROM votes WHERE user_id = ? AND answer_id = ?";
        List<Vote> result = jdbcTemplate.query(sql, voteRowMapper, userId, answerId);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public List<Long> findQuestionIdsUpvotedByUser(Long userId) {
        String sql = "SELECT question_id FROM votes WHERE user_id = ? AND question_id IS NOT NULL AND vote_type = 1";
        return jdbcTemplate.queryForList(sql, Long.class, userId);
    }

    public List<Long> findAnswerIdsUpvotedByUser(Long userId) {
        String sql = "SELECT answer_id FROM votes WHERE user_id = ? AND answer_id IS NOT NULL AND vote_type = 1";
        return jdbcTemplate.queryForList(sql, Long.class, userId);
    }
}
