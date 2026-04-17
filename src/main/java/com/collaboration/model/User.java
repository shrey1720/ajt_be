package com.collaboration.model;

import java.sql.Timestamp;

public class User {
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String role;
    private Integer reputation;
    private boolean emailVerified;
    private String verificationToken;
    private String resetPasswordToken;
    private Timestamp resetTokenExpiresAt;
    private String refreshToken;
    private Timestamp refreshTokenExpiresAt;
    private Timestamp createdAt;

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    // Aliases for JSON mapping from frontend "password" key
    public String getPassword() { return passwordHash; }
    public void setPassword(String password) { this.passwordHash = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getReputation() { return reputation; }
    public void setReputation(Integer reputation) { this.reputation = reputation; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }

    public String getResetPasswordToken() { return resetPasswordToken; }
    public void setResetPasswordToken(String resetPasswordToken) { this.resetPasswordToken = resetPasswordToken; }

    public Timestamp getResetTokenExpiresAt() { return resetTokenExpiresAt; }
    public void setResetTokenExpiresAt(Timestamp resetTokenExpiresAt) { this.resetTokenExpiresAt = resetTokenExpiresAt; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public Timestamp getRefreshTokenExpiresAt() { return refreshTokenExpiresAt; }
    public void setRefreshTokenExpiresAt(Timestamp refreshTokenExpiresAt) { this.refreshTokenExpiresAt = refreshTokenExpiresAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
