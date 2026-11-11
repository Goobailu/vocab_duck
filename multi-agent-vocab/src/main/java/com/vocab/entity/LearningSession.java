package com.vocab.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_sessions", indexes = {
    @Index(name = "idx_session_user", columnList = "user_id")
})
public class LearningSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String sessionType;

    private LocalDateTime startedAt = LocalDateTime.now();

    private LocalDateTime endedAt;

    private Integer wordsStudied = 0;

    private Integer wordsCorrect = 0;

    private Double avgResponseTime;

    private Double sessionScore;

    public LearningSession() {}

    public LearningSession(Long id, Long userId, String sessionType, LocalDateTime startedAt,
                           LocalDateTime endedAt, Integer wordsStudied, Integer wordsCorrect,
                           Double avgResponseTime, Double sessionScore) {
        this.id = id;
        this.userId = userId;
        this.sessionType = sessionType;
        this.startedAt = startedAt != null ? startedAt : LocalDateTime.now();
        this.endedAt = endedAt;
        this.wordsStudied = wordsStudied != null ? wordsStudied : 0;
        this.wordsCorrect = wordsCorrect != null ? wordsCorrect : 0;
        this.avgResponseTime = avgResponseTime;
        this.sessionScore = sessionScore;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public Integer getWordsStudied() { return wordsStudied; }
    public void setWordsStudied(Integer wordsStudied) { this.wordsStudied = wordsStudied; }
    public Integer getWordsCorrect() { return wordsCorrect; }
    public void setWordsCorrect(Integer wordsCorrect) { this.wordsCorrect = wordsCorrect; }
    public Double getAvgResponseTime() { return avgResponseTime; }
    public void setAvgResponseTime(Double avgResponseTime) { this.avgResponseTime = avgResponseTime; }
    public Double getSessionScore() { return sessionScore; }
    public void setSessionScore(Double sessionScore) { this.sessionScore = sessionScore; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long userId;
        private String sessionType;
        private LocalDateTime startedAt = LocalDateTime.now();
        private LocalDateTime endedAt;
        private Integer wordsStudied = 0;
        private Integer wordsCorrect = 0;
        private Double avgResponseTime;
        private Double sessionScore;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder sessionType(String sessionType) { this.sessionType = sessionType; return this; }
        public Builder startedAt(LocalDateTime startedAt) { this.startedAt = startedAt; return this; }
        public Builder endedAt(LocalDateTime endedAt) { this.endedAt = endedAt; return this; }
        public Builder wordsStudied(Integer wordsStudied) { this.wordsStudied = wordsStudied; return this; }
        public Builder wordsCorrect(Integer wordsCorrect) { this.wordsCorrect = wordsCorrect; return this; }
        public Builder avgResponseTime(Double avgResponseTime) { this.avgResponseTime = avgResponseTime; return this; }
        public Builder sessionScore(Double sessionScore) { this.sessionScore = sessionScore; return this; }

        public LearningSession build() {
            return new LearningSession(id, userId, sessionType, startedAt, endedAt,
                    wordsStudied, wordsCorrect, avgResponseTime, sessionScore);
        }
    }
}
