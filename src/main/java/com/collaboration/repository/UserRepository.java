package com.collaboration.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.collaboration.model.User;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setReputation(rs.getInt("reputation"));
        u.setEmailVerified(rs.getBoolean("email_verified"));
        u.setVerificationToken(rs.getString("verification_token"));
        u.setRefreshToken(rs.getString("refresh_token"));
        u.setRefreshTokenExpiresAt(rs.getTimestamp("refresh_token_expires_at"));
        u.setResetPasswordToken(rs.getString("reset_password_token"));
        u.setResetTokenExpiresAt(rs.getTimestamp("reset_token_expires_at"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    };

    public User save(User user) {
        String sql = "INSERT INTO users (username, email, password_hash, role, reputation, email_verified, verification_token, refresh_token, refresh_token_expires_at, reset_password_token, reset_token_expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        Long id = jdbcTemplate.queryForObject(sql, Long.class,
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getReputation(),
                user.isEmailVerified(),
                user.getVerificationToken(),
                user.getRefreshToken(),
                user.getRefreshTokenExpiresAt(),
                user.getResetPasswordToken(),
                user.getResetTokenExpiresAt());
        user.setId(id);
        return user;
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, username);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, email);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByRefreshToken(String refreshToken) {
        String sql = "SELECT * FROM users WHERE refresh_token = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, refreshToken);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByVerificationToken(String verificationToken) {
        String sql = "SELECT * FROM users WHERE verification_token = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, verificationToken);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByResetPasswordToken(String resetToken) {
        String sql = "SELECT * FROM users WHERE reset_password_token = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, resetToken);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findById(Long userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, userId);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void updateRefreshToken(Long userId, String refreshToken, Timestamp expiresAt) {
        String sql = "UPDATE users SET refresh_token = ?, refresh_token_expires_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, refreshToken, expiresAt, userId);
    }

    public void clearRefreshToken(Long userId) {
        String sql = "UPDATE users SET refresh_token = NULL, refresh_token_expires_at = NULL WHERE id = ?";
        jdbcTemplate.update(sql, userId);
    }

    public void updateEmailVerification(User user) {
        String sql = "UPDATE users SET email_verified = ?, verification_token = NULL WHERE id = ?";
        jdbcTemplate.update(sql, user.isEmailVerified(), user.getId());
    }

    public void updatePasswordReset(User user) {
        String sql = "UPDATE users SET password_hash = ?, reset_password_token = NULL, reset_token_expires_at = NULL WHERE id = ?";
        jdbcTemplate.update(sql, user.getPasswordHash(), user.getId());
    }

    public void savePasswordResetToken(Long userId, String resetToken, Timestamp expiresAt) {
        String sql = "UPDATE users SET reset_password_token = ?, reset_token_expires_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, resetToken, expiresAt, userId);
    }

    public void saveVerificationToken(Long userId, String verificationToken) {
        String sql = "UPDATE users SET verification_token = ? WHERE id = ?";
        jdbcTemplate.update(sql, verificationToken, userId);
    }

    public List<User> findTopUsers() {
        String sql = "SELECT * FROM users ORDER BY reputation DESC LIMIT 5";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    public void updateReputation(Long userId, int amount) {
        jdbcTemplate.update("UPDATE users SET reputation = reputation + ? WHERE id = ?", amount, userId);
    }
}
