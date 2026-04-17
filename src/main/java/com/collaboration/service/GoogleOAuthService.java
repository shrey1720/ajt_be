package com.collaboration.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.collaboration.model.User;
import com.collaboration.repository.UserRepository;

@Service
public class GoogleOAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final RestTemplate restTemplate;

    public GoogleOAuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${google.oauth.client-id:}") String clientId,
            @Value("${google.oauth.client-secret:}") String clientSecret,
            @Value("${google.oauth.redirect-uri:http://localhost:8080/api/oauth2/callback/google}") String redirectUri) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.restTemplate = new RestTemplate();
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }

    public GoogleUserInfo verifyIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            return null;
        }

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://oauth2.googleapis.com/tokeninfo?id_token={idToken}",
                    HttpMethod.GET,
                    null,
                    Map.class,
                    idToken);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return null;
            }

            Map<String, Object> payload = response.getBody();
            if (payload == null) {
                return null;
            }

            String audience = String.valueOf(payload.get("aud"));
            String emailVerifiedValue = String.valueOf(payload.get("email_verified"));
            boolean emailVerified = "true".equalsIgnoreCase(emailVerifiedValue) || "1".equals(emailVerifiedValue);

            if (!emailVerified) {
                return null;
            }
            if (clientId != null && !clientId.isBlank() && !clientId.equals(audience)) {
                return null;
            }

            return new GoogleUserInfo(
                    String.valueOf(payload.get("sub")),
                    String.valueOf(payload.get("email")),
                    String.valueOf(payload.get("name")),
                    emailVerified
            );
        } catch (Exception ex) {
            return null;
        }
    }

    public GoogleUserInfo exchangeCodeForIdToken(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        if (!isConfigured()) {
            throw new IllegalStateException("Google OAuth client credentials are not configured.");
        }

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("code", code);
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("redirect_uri", redirectUri);
        requestBody.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token",
                    request,
                    Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return null;
            }

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("id_token")) {
                return null;
            }

            return verifyIdToken(String.valueOf(body.get("id_token")));
        } catch (Exception ex) {
            return null;
        }
    }

    public User loadOrCreateUser(GoogleUserInfo googleUserInfo) {
        Optional<User> existing = userRepository.findByEmail(googleUserInfo.email());
        if (existing.isPresent()) {
            User user = existing.get();
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                userRepository.updateEmailVerification(user);
            }
            return user;
        }

        String candidateUsername = deriveUsername(googleUserInfo.email());
        String finalUsername = candidateUsername;
        int suffix = 1;
        while (userRepository.findByUsername(finalUsername).isPresent()) {
            finalUsername = candidateUsername + suffix;
            suffix++;
        }

        User user = new User();
        user.setUsername(finalUsername);
        user.setEmail(googleUserInfo.email());
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole("USER");
        user.setReputation(0);
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setRefreshToken(null);
        user.setResetPasswordToken(null);
        user.setRefreshTokenExpiresAt(null);
        userRepository.save(user);
        return user;
    }

    private String deriveUsername(String email) {
        if (email == null || email.isBlank()) {
            return "google-user";
        }
        String base = email.split("@")[0].replaceAll("[^A-Za-z0-9_.-]", "");
        return base.isBlank() ? "google-user" : base;
    }

    public record GoogleUserInfo(String googleId, String email, String fullName, boolean emailVerified) {}
}
