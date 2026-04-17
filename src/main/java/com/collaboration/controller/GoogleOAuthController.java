package com.collaboration.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.collaboration.model.User;
import com.collaboration.repository.UserRepository;
import com.collaboration.security.JwtService;
import com.collaboration.service.EmailService;
import com.collaboration.service.GoogleOAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
public class GoogleOAuthController {

    private final GoogleOAuthService googleOAuthService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final String googleClientId;
    private final String redirectUri;

    public GoogleOAuthController(
            GoogleOAuthService googleOAuthService,
            UserRepository userRepository,
            JwtService jwtService,
            EmailService emailService,
            @Value("${google.oauth.client-id:}") String googleClientId,
            @Value("${google.oauth.redirect-uri:https://ajt-be-3.onrender.com/api/oauth2/callback/google}") String redirectUri) {
        this.googleOAuthService = googleOAuthService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.googleClientId = googleClientId;
        this.redirectUri = redirectUri;
    }

    @GetMapping("/oauth2/authorize/google")
    public ResponseEntity<Void> redirectToGoogle() {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent");

        return ResponseEntity.status(HttpStatus.FOUND).location(uriBuilder.build().toUri()).build();
    }

    @GetMapping("/oauth2/callback/google")
    public ResponseEntity<String> handleGoogleCallback(@RequestParam(value = "code", required = false) String code) {
        if (code == null || code.isBlank()) {
            return buildErrorPage("Authorization code is required");
        }

        GoogleOAuthService.GoogleUserInfo googleUser = googleOAuthService.exchangeCodeForIdToken(code);
        if (googleUser == null) {
            return buildErrorPage("Unable to verify Google login. Please try again.");
        }

        User user = googleOAuthService.loadOrCreateUser(googleUser);
        String token = jwtService.generateToken(user);
        String refreshToken = UUID.randomUUID().toString();
        userRepository.updateRefreshToken(user.getId(), refreshToken, Timestamp.from(Instant.now().plus(30, ChronoUnit.DAYS)));

        Map<String, Object> authResponse = buildAuthResponse(user, token, refreshToken);
        String frontendRedirectUrl = "http://localhost:8000/index.html";

        String payload;
        try {
            payload = URLEncoder.encode(new ObjectMapper().writeValueAsString(authResponse), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return buildErrorPage("Failed to prepare login redirect.");
        }

        String html = "<html><head><meta charset=\"utf-8\"></head><body>"
                + "<script>window.location.replace('" + frontendRedirectUrl + "?payload=" + payload + "');</script>"
                + "</body></html>";

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    private ResponseEntity<String> buildErrorPage(String errorMessage) {
        String html = "<html><head><meta charset=\"utf-8\"></head><body>"
                + "<h1>Google Login Failed</h1>"
                + "<p>" + errorMessage + "</p>"
                + "<p><a href=\"http://localhost:8000/login.html\">Return to login</a></p>"
                + "</body></html>";
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.TEXT_HTML).body(html);
    }

    @PostMapping("/oauth2/google/token")
    public ResponseEntity<?> loginWithGoogleToken(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "idToken is required"));
        }

        GoogleOAuthService.GoogleUserInfo googleUser = googleOAuthService.verifyIdToken(idToken);
        if (googleUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid Google ID token"));
        }

        User user = googleOAuthService.loadOrCreateUser(googleUser);
        String token = jwtService.generateToken(user);
        String refreshToken = UUID.randomUUID().toString();
        userRepository.updateRefreshToken(user.getId(), refreshToken, Timestamp.from(Instant.now().plus(30, ChronoUnit.DAYS)));

        return ResponseEntity.ok(buildAuthResponse(user, token, refreshToken));
    }

    private Map<String, Object> buildAuthResponse(User user, String token, String refreshToken) {
        return Map.of(
                "token", token,
                "refreshToken", refreshToken,
                "user", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail() == null ? "" : user.getEmail(),
                        "role", user.getRole() == null ? "USER" : user.getRole(),
                        "reputation", user.getReputation() == null ? 0 : user.getReputation()
                )
        );
    }
}
