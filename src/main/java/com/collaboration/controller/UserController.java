package com.collaboration.controller;

import com.collaboration.model.User;
import com.collaboration.repository.UserRepository;
import com.collaboration.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "Username already exists"));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);
        return ResponseEntity.ok(buildAuthResponse(savedUser, token));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginReq) {
        Optional<User> userOpt = userRepository.findByUsername(loginReq.getUsername());
        if (userOpt.isPresent() && passwordEncoder.matches(loginReq.getPassword(), userOpt.get().getPassword())) {
            User user = userOpt.get();
            String token = jwtService.generateToken(user);
            return ResponseEntity.ok(buildAuthResponse(user, token));
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }

    @GetMapping("/users/top")
    public ResponseEntity<?> getTopUsers() {
        return ResponseEntity.ok(userRepository.findTopUsers());
    }

    private Map<String, Object> buildAuthResponse(User user, String token) {
        Integer reputation = user.getReputation();
        return Map.of(
                "token", token,
                "user", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail() == null ? "" : user.getEmail(),
                        "role", user.getRole() == null ? "USER" : user.getRole(),
                        "reputation", reputation != null ? reputation : 0
                )
        );
    }
}
