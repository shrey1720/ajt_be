package com.collaboration.controller;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.collaboration.model.User;
import com.collaboration.repository.UserRepository;
import com.collaboration.security.JwtService;
import com.collaboration.service.EmailService;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "Username already exists"));
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already registered"));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        user.setReputation(0);
        user.setEmailVerified(false);
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        User savedUser = userRepository.save(user);
        emailService.sendVerificationToken(savedUser.getEmail(), verificationToken);

        String token = jwtService.generateToken(savedUser);
        String refreshToken = UUID.randomUUID().toString();
        userRepository.updateRefreshToken(savedUser.getId(), refreshToken, Timestamp.from(Instant.now().plus(90, ChronoUnit.DAYS)));

        Map<String, Object> response = buildAuthResponse(savedUser, token);
        response.put("refreshToken", refreshToken);
        response.put("verificationToken", verificationToken);
        response.put("message", "Registration successful. Check logs for verification link.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginReq) {
        Optional<User> userOpt = userRepository.findByUsername(loginReq.getUsername());
        if (userOpt.isEmpty() || !passwordEncoder.matches(loginReq.getPassword(), userOpt.get().getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        User user = userOpt.get();
        if (!user.isEmailVerified()) {
            return ResponseEntity.status(403).body(Map.of("error", "Email not verified"));
        }

        String token = jwtService.generateToken(user);
        String refreshToken = UUID.randomUUID().toString();
        userRepository.updateRefreshToken(user.getId(), refreshToken, Timestamp.from(Instant.now().plus(90, ChronoUnit.DAYS)));

        Map<String, Object> response = buildAuthResponse(user, token);
        response.put("refreshToken", refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh token is required"));
        }

        Optional<User> userOpt = userRepository.findByRefreshToken(refreshToken);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
        }

        User user = userOpt.get();
        if (user.getRefreshTokenExpiresAt() == null || user.getRefreshTokenExpiresAt().before(Timestamp.from(Instant.now()))) {
            userRepository.clearRefreshToken(user.getId());
            return ResponseEntity.status(401).body(Map.of("error", "Refresh token expired"));
        }

        String token = jwtService.generateToken(user);
        String newRefreshToken = UUID.randomUUID().toString();
        userRepository.updateRefreshToken(user.getId(), newRefreshToken, Timestamp.from(Instant.now().plus(90, ChronoUnit.DAYS)));

        Map<String, Object> response = buildAuthResponse(user, token);
        response.put("refreshToken", newRefreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No active user"));
        }
        userRepository.findByUsername(authentication.getName()).ifPresent(user -> userRepository.clearRefreshToken(user.getId()));
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        Optional<User> userOpt = userRepository.findByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }
        return ResponseEntity.ok(buildAuthResponse(userOpt.get(), null));
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<?> requestPasswordReset(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "If this email exists, a reset link has been sent."));
        }

        User user = userOpt.get();
        String resetToken = UUID.randomUUID().toString();
        userRepository.savePasswordResetToken(user.getId(), resetToken, Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)));
        emailService.sendPasswordResetToken(user.getEmail(), resetToken);
        return ResponseEntity.ok(Map.of("message", "Password reset requested. Check logs for the reset token.", "resetToken", resetToken));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String resetToken = body.get("token");
        String password = body.get("password");
        if (resetToken == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reset token and password are required"));
        }

        Optional<User> userOpt = userRepository.findByResetPasswordToken(resetToken);
        if (userOpt.isEmpty() || userOpt.get().getResetTokenExpiresAt() == null || userOpt.get().getResetTokenExpiresAt().before(Timestamp.from(Instant.now()))) {
            return ResponseEntity.status(400).body(Map.of("error", "Reset token is invalid or expired"));
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.updatePasswordReset(user);
        return ResponseEntity.ok(Map.of("message", "Password successfully updated"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        Optional<User> userOpt = userRepository.findByVerificationToken(token);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "Verification token is invalid"));
        }

        User user = userOpt.get();
        user.setEmailVerified(true);
        userRepository.updateEmailVerification(user);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "If this email exists, a verification link has been sent."));
        }

        User user = userOpt.get();
        if (user.isEmailVerified()) {
            return ResponseEntity.ok(Map.of("message", "Email is already verified."));
        }

        String verificationToken = UUID.randomUUID().toString();
        userRepository.saveVerificationToken(user.getId(), verificationToken);
        emailService.sendVerificationToken(user.getEmail(), verificationToken);
        return ResponseEntity.ok(Map.of("message", "Verification link resent. Check logs for the token.", "verificationToken", verificationToken));
    }

    @GetMapping("/users/top")
    public ResponseEntity<?> getTopUsers() {
        return ResponseEntity.ok(userRepository.findTopUsers());
    }

    private Map<String, Object> buildAuthResponse(User user, String token) {
        Integer reputation = user.getReputation();
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail() == null ? "" : user.getEmail(),
                "role", user.getRole() == null ? "USER" : user.getRole(),
                "reputation", reputation != null ? reputation : 0
        ));
        return response;
    }
}
