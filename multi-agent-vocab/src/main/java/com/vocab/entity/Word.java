package com.vocab.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "words")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String word;

    @Column(length = 100)
    private String phonetic;

    @Column(name = "part_of_speech", length = 50)
    private String partOfSpeech;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String definitionEn;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String definitionCn;

    private Integer difficulty = 3;

    private Integer frequencyRank;

    /** JSON string: ["cet4","cet6","kaoyan"] */
    @Column(columnDefinition = "JSON")
    private String tags;

    /** JSON string: [{en,cn}, ...] */
    @Column(columnDefinition = "JSON")
    private String exampleSentences;

    @Column(columnDefinition = "TEXT")
    private String etymology;

    /** JSON string: ["synonym1", ...] */
    @Column(columnDefinition = "JSON")
    private String synonyms;

    /** JSON string: ["antonym1", ...] */
    @Column(columnDefinition = "JSON")
    private String antonyms;

    public Word() {}

    public Word(Long id, String word, String phonetic, String partOfSpeech, String definitionEn, String definitionCn,
                Integer difficulty, Integer frequencyRank, String tags, String exampleSentences, String etymology,
                String synonyms, String antonyms) {
        this.id = id;
        this.word = word;
        this.phonetic = phonetic;
        this.partOfSpeech = partOfSpeech;
        this.definitionEn = definitionEn;
        this.definitionCn = definitionCn;
        this.difficulty = difficulty;
        this.frequencyRank = frequencyRank;
        this.tags = tags;
        this.exampleSentences = exampleSentences;
        this.etymology = etymology;
        this.synonyms = synonyms;
        this.antonyms = antonyms;
    }

    // Getters
    public Long getId() { return id; }
    public String getWord() { return word; }
    public String getPhonetic() { return phonetic; }
    public String getPartOfSpeech() { return partOfSpeech; }
    public String getDefinitionEn() { return definitionEn; }
    public String getDefinitionCn() { return definitionCn; }
    public Integer getDifficulty() { return difficulty; }
    public Integer getFrequencyRank() { return frequencyRank; }
    public String getTags() { return tags; }
    public String getExampleSentences() { return exampleSentences; }
    public String getEtymology() { return etymology; }
    public String getSynonyms() { return synonyms; }
    public String getAntonyms() { return antonyms; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setWord(String word) { this.word = word; }
    public void setPhonetic(String phonetic) { this.phonetic = phonetic; }
    public void setPartOfSpeech(String partOfSpeech) { this.partOfSpeech = partOfSpeech; }
    public void setDefinitionEn(String definitionEn) { this.definitionEn = definitionEn; }
    public void setDefinitionCn(String definitionCn) { this.definitionCn = definitionCn; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public void setFrequencyRank(Integer frequencyRank) { this.frequencyRank = frequencyRank; }
    public void setTags(String tags) { this.tags = tags; }
    public void setExampleSentences(String exampleSentences) { this.exampleSentences = exampleSentences; }
    public void setEtymology(String etymology) { this.etymology = etymology; }
    public void setSynonyms(String synonyms) { this.synonyms = synonyms; }
    public void setAntonyms(String antonyms) { this.antonyms = antonyms; }

    // Builder pattern
    public static WordBuilder builder() { return new WordBuilder(); }

    public static class WordBuilder {
        private Long id;
        private String word;
        private String phonetic;
        private String partOfSpeech;
        private String definitionEn;
        private String definitionCn;
        private Integer difficulty = 3;
        private Integer frequencyRank;
        private String tags;
        private String exampleSentences;
        private String etymology;
        private String synonyms;
        private String antonyms;

        public WordBuilder id(Long id) { this.id = id; return this; }
        public WordBuilder word(String word) { this.word = word; return this; }
        public WordBuilder phonetic(String phonetic) { this.phonetic = phonetic; return this; }
        public WordBuilder partOfSpeech(String partOfSpeech) { this.partOfSpeech = partOfSpeech; return this; }
        public WordBuilder definitionEn(String definitionEn) { this.definitionEn = definitionEn; return this; }
        public WordBuilder definitionCn(String definitionCn) { this.definitionCn = definitionCn; return this; }
        public WordBuilder difficulty(Integer difficulty) { this.difficulty = difficulty; return this; }
        public WordBuilder frequencyRank(Integer frequencyRank) { this.frequencyRank = frequencyRank; return this; }
        public WordBuilder tags(String tags) { this.tags = tags; return this; }
        public WordBuilder exampleSentences(String exampleSentences) { this.exampleSentences = exampleSentences; return this; }
        public WordBuilder etymology(String etymology) { this.etymology = etymology; return this; }
        public WordBuilder synonyms(String synonyms) { this.synonyms = synonyms; return this; }
        public WordBuilder antonyms(String antonyms) { this.antonyms = antonyms; return this; }

        public Word build() {
            return new Word(id, word, phonetic, partOfSpeech, definitionEn, definitionCn, difficulty,
                    frequencyRank, tags, exampleSentences, etymology, synonyms, antonyms);
        }
    }
}
