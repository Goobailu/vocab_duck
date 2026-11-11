package com.vocab.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_records", indexes = {
    @Index(name = "idx_review_user", columnList = "user_id")
})
public class ReviewRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    @Column(name = "user_word_id", nullable = false)
    private Long userWordId;

    private Long sessionId;

    private Integer quality;

    private Boolean correct = false;

    private Double responseTime;

    @Column(length = 20)
    private String reviewType;

    private Double prevMastery;
    private Integer prevInterval;
    private Double prevEaseFactor;

    private Double newMastery;
    private Integer newInterval;
    private Double newEaseFactor;

    private LocalDateTime reviewedAt = LocalDateTime.now();

    public ReviewRecord() {}

    public ReviewRecord(Long id, Long userId, Long wordId, Long userWordId, Long sessionId,
                        Integer quality, Boolean correct, Double responseTime, String reviewType,
                        Double prevMastery, Integer prevInterval, Double prevEaseFactor,
                        Double newMastery, Integer newInterval, Double newEaseFactor,
                        LocalDateTime reviewedAt) {
        this.id = id;
        this.userId = userId;
        this.wordId = wordId;
        this.userWordId = userWordId;
        this.sessionId = sessionId;
        this.quality = quality;
        this.correct = correct != null ? correct : false;
        this.responseTime = responseTime;
        this.reviewType = reviewType;
        this.prevMastery = prevMastery;
        this.prevInterval = prevInterval;
        this.prevEaseFactor = prevEaseFactor;
        this.newMastery = newMastery;
        this.newInterval = newInterval;
        this.newEaseFactor = newEaseFactor;
        this.reviewedAt = reviewedAt != null ? reviewedAt : LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getWordId() { return wordId; }
    public void setWordId(Long wordId) { this.wordId = wordId; }
    public Long getUserWordId() { return userWordId; }
    public void setUserWordId(Long userWordId) { this.userWordId = userWordId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Integer getQuality() { return quality; }
    public void setQuality(Integer quality) { this.quality = quality; }
    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }
    public Double getResponseTime() { return responseTime; }
    public void setResponseTime(Double responseTime) { this.responseTime = responseTime; }
    public String getReviewType() { return reviewType; }
    public void setReviewType(String reviewType) { this.reviewType = reviewType; }
    public Double getPrevMastery() { return prevMastery; }
    public void setPrevMastery(Double prevMastery) { this.prevMastery = prevMastery; }
    public Integer getPrevInterval() { return prevInterval; }
    public void setPrevInterval(Integer prevInterval) { this.prevInterval = prevInterval; }
    public Double getPrevEaseFactor() { return prevEaseFactor; }
    public void setPrevEaseFactor(Double prevEaseFactor) { this.prevEaseFactor = prevEaseFactor; }
    public Double getNewMastery() { return newMastery; }
    public void setNewMastery(Double newMastery) { this.newMastery = newMastery; }
    public Integer getNewInterval() { return newInterval; }
    public void setNewInterval(Integer newInterval) { this.newInterval = newInterval; }
    public Double getNewEaseFactor() { return newEaseFactor; }
    public void setNewEaseFactor(Double newEaseFactor) { this.newEaseFactor = newEaseFactor; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long userId;
        private Long wordId;
        private Long userWordId;
        private Long sessionId;
        private Integer quality;
        private Boolean correct = false;
        private Double responseTime;
        private String reviewType;
        private Double prevMastery;
        private Integer prevInterval;
        private Double prevEaseFactor;
        private Double newMastery;
        private Integer newInterval;
        private Double newEaseFactor;
        private LocalDateTime reviewedAt = LocalDateTime.now();

        public Builder id(Long id) { this.id = id; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder wordId(Long wordId) { this.wordId = wordId; return this; }
        public Builder userWordId(Long userWordId) { this.userWordId = userWordId; return this; }
        public Builder sessionId(Long sessionId) { this.sessionId = sessionId; return this; }
        public Builder quality(Integer quality) { this.quality = quality; return this; }
        public Builder correct(Boolean correct) { this.correct = correct; return this; }
        public Builder responseTime(Double responseTime) { this.responseTime = responseTime; return this; }
        public Builder reviewType(String reviewType) { this.reviewType = reviewType; return this; }
        public Builder prevMastery(Double prevMastery) { this.prevMastery = prevMastery; return this; }
        public Builder prevInterval(Integer prevInterval) { this.prevInterval = prevInterval; return this; }
        public Builder prevEaseFactor(Double prevEaseFactor) { this.prevEaseFactor = prevEaseFactor; return this; }
        public Builder newMastery(Double newMastery) { this.newMastery = newMastery; return this; }
        public Builder newInterval(Integer newInterval) { this.newInterval = newInterval; return this; }
        public Builder newEaseFactor(Double newEaseFactor) { this.newEaseFactor = newEaseFactor; return this; }
        public Builder reviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; return this; }

        public ReviewRecord build() {
            return new ReviewRecord(id, userId, wordId, userWordId, sessionId,
                    quality, correct, responseTime, reviewType,
                    prevMastery, prevInterval, prevEaseFactor,
                    newMastery, newInterval, newEaseFactor, reviewedAt);
        }
    }
}
