package com.vocab.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Caches generated content (learning cards, image prompts, quizzes) keyed by
 * word_id + content_type to avoid redundant LLM calls.
 */
@Entity
@Table(name = "generated_content", indexes = {
    @Index(name = "idx_content_word", columnList = "word_id")
})
public class GeneratedContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", insertable = false, updatable = false)
    private Word word;

    @Column(name = "content_type", length = 30, nullable = false)
    private String contentType;

    @Column(name = "content_text", nullable = false, columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "content_json", columnDefinition = "JSON")
    private String contentJson;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    private Integer difficulty;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public GeneratedContent() {}

    // --- Getters ---
    public Long getId() { return id; }
    public Long getWordId() { return wordId; }
    public Word getWord() { return word; }
    public String getContentType() { return contentType; }
    public String getContentText() { return contentText; }
    public String getContentJson() { return contentJson; }
    public String getImageUrl() { return imageUrl; }
    public Integer getDifficulty() { return difficulty; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // --- Setters ---
    public void setId(Long id) { this.id = id; }
    public void setWordId(Long wordId) { this.wordId = wordId; }
    public void setWord(Word word) { this.word = word; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public void setContentJson(String contentJson) { this.contentJson = contentJson; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // --- Builder ---
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long wordId;
        private Word word;
        private String contentType;
        private String contentText;
        private String contentJson;
        private String imageUrl;
        private Integer difficulty;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder wordId(Long wordId) { this.wordId = wordId; return this; }
        public Builder word(Word word) { this.word = word; return this; }
        public Builder contentType(String contentType) { this.contentType = contentType; return this; }
        public Builder contentText(String contentText) { this.contentText = contentText; return this; }
        public Builder contentJson(String contentJson) { this.contentJson = contentJson; return this; }
        public Builder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public Builder difficulty(Integer difficulty) { this.difficulty = difficulty; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public GeneratedContent build() {
            GeneratedContent gc = new GeneratedContent();
            gc.wordId = this.wordId;
            gc.word = this.word;
            gc.contentType = this.contentType;
            gc.contentText = this.contentText;
            gc.contentJson = this.contentJson;
            gc.imageUrl = this.imageUrl;
            gc.difficulty = this.difficulty;
            gc.createdAt = this.createdAt;
            return gc;
        }
    }
}
