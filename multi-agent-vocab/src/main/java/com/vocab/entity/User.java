package com.vocab.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 120)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 50)
    private String targetExam = "cet6";

    private Integer targetVocabSize = 6000;

    private Integer dailyNewWords = 20;

    private Integer difficultyLevel = 3;

    /** Personalized memory decay factor */
    private Double memoryStrength = 1.0;

    /** Running average recall rate */
    private Double avgRecallRate = 0.0;

    @Column(columnDefinition = "tinyint(1) default 1")
    private Boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastLogin = LocalDateTime.now();

    private Integer streakDays = 0;

    /** Selected vocabulary library for learning (cet4/cet6/kaoyan/ielts/toefl) */
    @Column(length = 50)
    private String selectedLibrary = null;

    /** Memory style: VISUAL, AUDITORY, SEMANTIC */
    @Column(length = 20)
    private String memoryStyle = "VISUAL";

    public User() {}

    public User(Long id, String username, String email, String password, String targetExam,
                Integer targetVocabSize, Integer dailyNewWords, Integer difficultyLevel,
                Double memoryStrength, Double avgRecallRate, Boolean active,
                LocalDateTime createdAt, LocalDateTime lastLogin, Integer streakDays,
                String selectedLibrary, String memoryStyle) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.targetExam = targetExam;
        this.targetVocabSize = targetVocabSize;
        this.dailyNewWords = dailyNewWords;
        this.difficultyLevel = difficultyLevel;
        this.memoryStrength = memoryStrength;
        this.avgRecallRate = avgRecallRate;
        this.active = active;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.streakDays = streakDays;
        this.selectedLibrary = selectedLibrary;
        this.memoryStyle = memoryStyle;
    }

    // Getters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getTargetExam() { return targetExam; }
    public Integer getTargetVocabSize() { return targetVocabSize; }
    public Integer getDailyNewWords() { return dailyNewWords; }
    public Integer getDifficultyLevel() { return difficultyLevel; }
    public Double getMemoryStrength() { return memoryStrength; }
    public Double getAvgRecallRate() { return avgRecallRate; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public Integer getStreakDays() { return streakDays; }
    public String getSelectedLibrary() { return selectedLibrary; }
    public String getMemoryStyle() { return memoryStyle; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setTargetExam(String targetExam) { this.targetExam = targetExam; }
    public void setTargetVocabSize(Integer targetVocabSize) { this.targetVocabSize = targetVocabSize; }
    public void setDailyNewWords(Integer dailyNewWords) { this.dailyNewWords = dailyNewWords; }
    public void setDifficultyLevel(Integer difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    public void setMemoryStrength(Double memoryStrength) { this.memoryStrength = memoryStrength; }
    public void setAvgRecallRate(Double avgRecallRate) { this.avgRecallRate = avgRecallRate; }
    public void setActive(Boolean active) { this.active = active; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    public void setStreakDays(Integer streakDays) { this.streakDays = streakDays; }
    public void setSelectedLibrary(String selectedLibrary) { this.selectedLibrary = selectedLibrary; }
    public void setMemoryStyle(String memoryStyle) { this.memoryStyle = memoryStyle; }

    // Builder pattern
    public static UserBuilder builder() { return new UserBuilder(); }

    public static class UserBuilder {
        private Long id;
        private String username;
        private String email;
        private String password;
        private String targetExam = "cet6";
        private Integer targetVocabSize = 6000;
        private Integer dailyNewWords = 20;
        private Integer difficultyLevel = 3;
        private Double memoryStrength = 1.0;
        private Double avgRecallRate = 0.0;
        private Boolean active = true;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime lastLogin = LocalDateTime.now();
        private Integer streakDays = 0;
        private String selectedLibrary = null;
        private String memoryStyle = "VISUAL";

        public UserBuilder id(Long id) { this.id = id; return this; }
        public UserBuilder username(String username) { this.username = username; return this; }
        public UserBuilder email(String email) { this.email = email; return this; }
        public UserBuilder password(String password) { this.password = password; return this; }
        public UserBuilder targetExam(String targetExam) { this.targetExam = targetExam; return this; }
        public UserBuilder targetVocabSize(Integer targetVocabSize) { this.targetVocabSize = targetVocabSize; return this; }
        public UserBuilder dailyNewWords(Integer dailyNewWords) { this.dailyNewWords = dailyNewWords; return this; }
        public UserBuilder difficultyLevel(Integer difficultyLevel) { this.difficultyLevel = difficultyLevel; return this; }
        public UserBuilder memoryStrength(Double memoryStrength) { this.memoryStrength = memoryStrength; return this; }
        public UserBuilder avgRecallRate(Double avgRecallRate) { this.avgRecallRate = avgRecallRate; return this; }
        public UserBuilder active(Boolean active) { this.active = active; return this; }
        public UserBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public UserBuilder lastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; return this; }
        public UserBuilder streakDays(Integer streakDays) { this.streakDays = streakDays; return this; }
        public UserBuilder selectedLibrary(String selectedLibrary) { this.selectedLibrary = selectedLibrary; return this; }
        public UserBuilder memoryStyle(String memoryStyle) { this.memoryStyle = memoryStyle; return this; }

        public User build() {
            return new User(id, username, email, password, targetExam, targetVocabSize, dailyNewWords,
                    difficultyLevel, memoryStrength, avgRecallRate, active, createdAt, lastLogin, streakDays,
                    selectedLibrary, memoryStyle);
        }
    }
}
