package com.vocab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocab.dto.RegisterRequest;
import com.vocab.entity.User;
import com.vocab.repository.UserRepository;
import com.vocab.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                          JwtTokenProvider tokenProvider, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Username already exists"));
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Email already registered"));
        }

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .targetExam(req.getTargetExam())
                .dailyNewWords(req.getDailyNewWords())
                .build();
        userRepository.save(user);
        return ResponseEntity.ok(toUserMap(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("detail", "Incorrect username or password"));
        }

        String token = tokenProvider.generateToken(user.getId(), user.getUsername());
        return ResponseEntity.ok(Map.of(
                "accessToken", token,
                "tokenType", "bearer",
                "user", toUserMap(user)
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@org.springframework.security.core.annotation.AuthenticationPrincipal User user) {
        return ResponseEntity.ok(toUserMap(user));
    }

    @PutMapping("/me/settings")
    public ResponseEntity<?> updateSettings(
            @org.springframework.security.core.annotation.AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> settings) {
        if (settings.containsKey("targetExam")) user.setTargetExam((String) settings.get("targetExam"));
        if (settings.containsKey("targetVocabSize")) user.setTargetVocabSize(toInt(settings.get("targetVocabSize")));
        if (settings.containsKey("dailyNewWords")) user.setDailyNewWords(toInt(settings.get("dailyNewWords")));
        if (settings.containsKey("difficultyLevel")) user.setDifficultyLevel(toInt(settings.get("difficultyLevel")));
        if (settings.containsKey("selectedLibrary")) {
            Object lib = settings.get("selectedLibrary");
            user.setSelectedLibrary(lib != null ? lib.toString() : null);
        }
        userRepository.save(user);
        return ResponseEntity.ok(toUserMap(user));
    }

    private Map<String, Object> toUserMap(User u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("username", u.getUsername());
        map.put("email", u.getEmail());
        map.put("targetExam", u.getTargetExam());
        map.put("targetVocabSize", u.getTargetVocabSize());
        map.put("dailyNewWords", u.getDailyNewWords());
        map.put("difficultyLevel", u.getDifficultyLevel());
        map.put("memoryStrength", u.getMemoryStrength());
        map.put("avgRecallRate", u.getAvgRecallRate());
        map.put("streakDays", u.getStreakDays());
        map.put("selectedLibrary", u.getSelectedLibrary());
        map.put("createdAt", u.getCreatedAt().toString());
        return map;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        return Integer.parseInt(obj.toString());
    }
}
