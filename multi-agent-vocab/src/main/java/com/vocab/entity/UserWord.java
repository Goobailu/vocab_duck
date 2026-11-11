package com.vocab.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_words", indexes = {
    @Index(name = "idx_user_word_user", columnList = "user_id"),
    @Index(name = "idx_user_word_word", columnList = "word_id"),
    @Index(name = "idx_user_word_next_review", columnList = "next_review")
})
public class UserWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", insertable = false, updatable = false)
    private Word word;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    // Mastery tracking (0.0 - 1.0)
    private Double masteryLevel = 0.0;

    private Double familiarity = 0.0;

    // SM-2 state
    private Double easeFactor = 2.5;

    private Integer intervalDays = 0;

    private Integer repetitions = 0;

    // Forgetting curve
    private LocalDateTime lastReviewed;
    private LocalDateTime nextReview;

    private Integer reviewCount = 0;

    private Integer correctCount = 0;

    private Integer wrongCount = 0;

    // Memory metrics
    private Double memoryStability = 0.0;

    private Double memoryRetrievability = 1.0;

    // Learning metadata
    private LocalDateTime firstLearned = LocalDateTime.now();
    private LocalDateTime learnDate;

    @Column(length = 20)
    private String status = "new";  // new, learning, review, mastered, forgotten

    @Column(length = 20)
    private String learningQueue = "new";  // new, daily, review

    private LocalDateTime createdAt = LocalDateTime.now();

    public UserWord() {}

    public UserWord(Long id, Long userId, Long wordId, Word word, User user, Double masteryLevel, Double familiarity,
                    Double easeFactor, Integer intervalDays, Integer repetitions, LocalDateTime lastReviewed,
                    LocalDateTime nextReview, Integer reviewCount, Integer correctCount, Integer wrongCount,
                    Double memoryStability, Double memoryRetrievability, LocalDateTime firstLearned, LocalDateTime learnDate,
                    String status, String learningQueue, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.wordId = wordId;
        this.word = word;
        this.user = user;
        this.masteryLevel = masteryLevel;
        this.familiarity = familiarity;
        this.easeFactor = easeFactor;
        this.intervalDays = intervalDays;
        this.repetitions = repetitions;
        this.lastReviewed = lastReviewed;
        this.nextReview = nextReview;
        this.reviewCount = reviewCount;
        this.correctCount = correctCount;
        this.wrongCount = wrongCount;
        this.memoryStability = memoryStability;
        this.memoryRetrievability = memoryRetrievability;
        this.firstLearned = firstLearned;
        this.learnDate = learnDate;
        this.status = status;
        this.learningQueue = learningQueue;
        this.createdAt = createdAt;
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getWordId() { return wordId; }
    public Word getWord() { return word; }
    public User getUser() { return user; }
    public Double getMasteryLevel() { return masteryLevel; }
    public Double getFamiliarity() { return familiarity; }
    public Double getEaseFactor() { return easeFactor; }
    public Integer getIntervalDays() { return intervalDays; }
    public Integer getRepetitions() { return repetitions; }
    public LocalDateTime getLastReviewed() { return lastReviewed; }
    public LocalDateTime getNextReview() { return nextReview; }
    public Integer getReviewCount() { return reviewCount; }
    public Integer getCorrectCount() { return correctCount; }
    public Integer getWrongCount() { return wrongCount; }
    public Double getMemoryStability() { return memoryStability; }
    public Double getMemoryRetrievability() { return memoryRetrievability; }
    public LocalDateTime getFirstLearned() { return firstLearned; }
    public LocalDateTime getLearnDate() { return learnDate; }
    public LocalDateTime getLastReviewDate() { return lastReviewed; }
    public String getStatus() { return status; }
    public String getLearningQueue() { return learningQueue; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setWordId(Long wordId) { this.wordId = wordId; }
    public void setWord(Word word) { this.word = word; }
    public void setUser(User user) { this.user = user; }
    public void setMasteryLevel(Double masteryLevel) { this.masteryLevel = masteryLevel; }
    public void setFamiliarity(Double familiarity) { this.familiarity = familiarity; }
    public void setEaseFactor(Double easeFactor) { this.easeFactor = easeFactor; }
    public void setIntervalDays(Integer intervalDays) { this.intervalDays = intervalDays; }
    public void setRepetitions(Integer repetitions) { this.repetitions = repetitions; }
    public void setLastReviewed(LocalDateTime lastReviewed) { this.lastReviewed = lastReviewed; }
    public void setNextReview(LocalDateTime nextReview) { this.nextReview = nextReview; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public void setCorrectCount(Integer correctCount) { this.correctCount = correctCount; }
    public void setWrongCount(Integer wrongCount) { this.wrongCount = wrongCount; }
    public void setMemoryStability(Double memoryStability) { this.memoryStability = memoryStability; }
    public void setMemoryRetrievability(Double memoryRetrievability) { this.memoryRetrievability = memoryRetrievability; }
    public void setFirstLearned(LocalDateTime firstLearned) { this.firstLearned = firstLearned; }
    public void setLearnDate(LocalDateTime learnDate) { this.learnDate = learnDate; }
    public void setLastReviewDate(LocalDateTime lastReviewed) { this.lastReviewed = lastReviewed; }
    public void setStatus(String status) { this.status = status; }
    public void setLearningQueue(String learningQueue) { this.learningQueue = learningQueue; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder pattern
    public static UserWordBuilder builder() { return new UserWordBuilder(); }

    public static class UserWordBuilder {
        private Long id;
        private Long userId;
        private Long wordId;
        private Word word;
        private User user;
        private Double masteryLevel = 0.0;
        private Double familiarity = 0.0;
        private Double easeFactor = 2.5;
        private Integer intervalDays = 0;
        private Integer repetitions = 0;
        private LocalDateTime lastReviewed;
        private LocalDateTime nextReview;
        private Integer reviewCount = 0;
        private Integer correctCount = 0;
        private Integer wrongCount = 0;
        private Double memoryStability = 0.0;
        private Double memoryRetrievability = 1.0;
        private LocalDateTime firstLearned = LocalDateTime.now();
        private LocalDateTime learnDate;
        private String status = "new";
        private String learningQueue = "new";
        private LocalDateTime createdAt = LocalDateTime.now();

        public UserWordBuilder id(Long id) { this.id = id; return this; }
        public UserWordBuilder userId(Long userId) { this.userId = userId; return this; }
        public UserWordBuilder wordId(Long wordId) { this.wordId = wordId; return this; }
        public UserWordBuilder word(Word word) { this.word = word; return this; }
        public UserWordBuilder user(User user) { this.user = user; return this; }
        public UserWordBuilder masteryLevel(Double masteryLevel) { this.masteryLevel = masteryLevel; return this; }
        public UserWordBuilder familiarity(Double familiarity) { this.familiarity = familiarity; return this; }
        public UserWordBuilder easeFactor(Double easeFactor) { this.easeFactor = easeFactor; return this; }
        public UserWordBuilder intervalDays(Integer intervalDays) { this.intervalDays = intervalDays; return this; }
        public UserWordBuilder repetitions(Integer repetitions) { this.repetitions = repetitions; return this; }
        public UserWordBuilder lastReviewed(LocalDateTime lastReviewed) { this.lastReviewed = lastReviewed; return this; }
        public UserWordBuilder nextReview(LocalDateTime nextReview) { this.nextReview = nextReview; return this; }
        public UserWordBuilder reviewCount(Integer reviewCount) { this.reviewCount = reviewCount; return this; }
        public UserWordBuilder correctCount(Integer correctCount) { this.correctCount = correctCount; return this; }
        public UserWordBuilder wrongCount(Integer wrongCount) { this.wrongCount = wrongCount; return this; }
        public UserWordBuilder memoryStability(Double memoryStability) { this.memoryStability = memoryStability; return this; }
        public UserWordBuilder memoryRetrievability(Double memoryRetrievability) { this.memoryRetrievability = memoryRetrievability; return this; }
        public UserWordBuilder firstLearned(LocalDateTime firstLearned) { this.firstLearned = firstLearned; return this; }
        public UserWordBuilder learnDate(LocalDateTime learnDate) { this.learnDate = learnDate; return this; }
        public UserWordBuilder status(String status) { this.status = status; return this; }
        public UserWordBuilder learningQueue(String learningQueue) { this.learningQueue = learningQueue; return this; }
        public UserWordBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public UserWord build() {
            return new UserWord(id, userId, wordId, word, user, masteryLevel, familiarity, easeFactor,
                    intervalDays, repetitions, lastReviewed, nextReview, reviewCount, correctCount,
                    wrongCount, memoryStability, memoryRetrievability, firstLearned, learnDate,
                    status, learningQueue, createdAt);
        }
    }
}
