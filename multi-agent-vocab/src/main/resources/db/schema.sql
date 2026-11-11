-- Multi-Agent Vocabulary Learning System - MySQL Schema
-- Run this script to initialize the database

CREATE DATABASE IF NOT EXISTS vocab_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE vocab_system;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(256) NOT NULL,
    target_exam VARCHAR(50) DEFAULT 'cet6',
    target_vocab_size INT DEFAULT 6000,
    daily_new_words INT DEFAULT 20,
    difficulty_level INT DEFAULT 3,
    memory_strength DOUBLE DEFAULT 1.0,
    avg_recall_rate DOUBLE DEFAULT 0.0,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_login DATETIME DEFAULT CURRENT_TIMESTAMP,
    streak_days INT DEFAULT 0,
    selected_library VARCHAR(50) DEFAULT NULL,
    INDEX idx_users_username (username),
    INDEX idx_users_email (email)
) ENGINE=InnoDB;

-- Vocabulary master table
CREATE TABLE IF NOT EXISTS words (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(100) NOT NULL UNIQUE,
    phonetic VARCHAR(100),
    part_of_speech VARCHAR(50),
    definition_en TEXT NOT NULL,
    definition_cn TEXT NOT NULL,
    difficulty INT DEFAULT 3,
    frequency_rank INT,
    tags JSON,
    example_sentences JSON,
    etymology TEXT,
    synonyms JSON,
    antonyms JSON,
    INDEX idx_words_word (word),
    INDEX idx_words_difficulty (difficulty)
) ENGINE=InnoDB;

-- Per-user word tracking
CREATE TABLE IF NOT EXISTS user_words (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    word_id BIGINT NOT NULL,
    mastery_level DOUBLE DEFAULT 0.0,
    familiarity DOUBLE DEFAULT 0.0,
    ease_factor DOUBLE DEFAULT 2.5,
    interval_days INT DEFAULT 0,
    repetitions INT DEFAULT 0,
    last_reviewed DATETIME,
    next_review DATETIME,
    review_count INT DEFAULT 0,
    correct_count INT DEFAULT 0,
    wrong_count INT DEFAULT 0,
    memory_stability DOUBLE DEFAULT 0.0,
    memory_retrievability DOUBLE DEFAULT 1.0,
    first_learned DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'new',
    learning_queue VARCHAR(20) DEFAULT 'new',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_word_user (user_id),
    INDEX idx_user_word_word (word_id),
    INDEX idx_user_word_next_review (next_review),
    INDEX idx_user_word_status (status),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (word_id) REFERENCES words(id)
) ENGINE=InnoDB;

-- Learning sessions
CREATE TABLE IF NOT EXISTS learning_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_type VARCHAR(20) NOT NULL,
    started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME,
    words_studied INT DEFAULT 0,
    words_correct INT DEFAULT 0,
    avg_response_time DOUBLE,
    session_score DOUBLE,
    INDEX idx_session_user (user_id),
    INDEX idx_session_started (started_at),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- Review records
CREATE TABLE IF NOT EXISTS review_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    word_id BIGINT NOT NULL,
    user_word_id BIGINT NOT NULL,
    session_id BIGINT,
    quality INT,
    correct BOOLEAN DEFAULT FALSE,
    response_time DOUBLE,
    review_type VARCHAR(20),
    prev_mastery DOUBLE,
    prev_interval INT,
    prev_ease_factor DOUBLE,
    new_mastery DOUBLE,
    new_interval INT,
    new_ease_factor DOUBLE,
    reviewed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_review_user (user_id),
    INDEX idx_review_reviewed (reviewed_at),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (word_id) REFERENCES words(id),
    FOREIGN KEY (user_word_id) REFERENCES user_words(id)
) ENGINE=InnoDB;

-- Chat messages
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    agent_type VARCHAR(30),
    metadata_json JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_user (user_id),
    INDEX idx_chat_created (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- Generated content cache
CREATE TABLE IF NOT EXISTS generated_content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word_id BIGINT NOT NULL,
    content_type VARCHAR(30) NOT NULL,
    content_text TEXT NOT NULL,
    content_json JSON,
    image_url VARCHAR(500),
    difficulty INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_content_word (word_id),
    FOREIGN KEY (word_id) REFERENCES words(id)
) ENGINE=InnoDB;
