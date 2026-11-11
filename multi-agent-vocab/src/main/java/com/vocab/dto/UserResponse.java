package com.vocab.dto;

import java.time.LocalDateTime;

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String targetExam;
    private Integer targetVocabSize;
    private Integer dailyNewWords;
    private Integer difficultyLevel;
    private Double memoryStrength;
    private Double avgRecallRate;
    private Integer streakDays;
    private LocalDateTime createdAt;

    public UserResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTargetExam() { return targetExam; }
    public void setTargetExam(String targetExam) { this.targetExam = targetExam; }
    public Integer getTargetVocabSize() { return targetVocabSize; }
    public void setTargetVocabSize(Integer targetVocabSize) { this.targetVocabSize = targetVocabSize; }
    public Integer getDailyNewWords() { return dailyNewWords; }
    public void setDailyNewWords(Integer dailyNewWords) { this.dailyNewWords = dailyNewWords; }
    public Integer getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(Integer difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    public Double getMemoryStrength() { return memoryStrength; }
    public void setMemoryStrength(Double memoryStrength) { this.memoryStrength = memoryStrength; }
    public Double getAvgRecallRate() { return avgRecallRate; }
    public void setAvgRecallRate(Double avgRecallRate) { this.avgRecallRate = avgRecallRate; }
    public Integer getStreakDays() { return streakDays; }
    public void setStreakDays(Integer streakDays) { this.streakDays = streakDays; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
