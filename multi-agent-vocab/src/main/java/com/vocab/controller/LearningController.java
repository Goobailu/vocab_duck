package com.vocab.controller;

import com.vocab.agent.ContentGenerationAgent;
import com.vocab.agent.ReviewSchedulingAgent;
import com.vocab.entity.LearningSession;
import com.vocab.entity.ReviewRecord;
import com.vocab.entity.User;
import com.vocab.entity.UserWord;
import com.vocab.entity.Word;
import com.vocab.repository.LearningSessionRepository;
import com.vocab.repository.ReviewRecordRepository;
import com.vocab.repository.UserRepository;
import com.vocab.repository.UserWordRepository;
import com.vocab.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/learning")
public class LearningController {

    private static final Logger log = LoggerFactory.getLogger(LearningController.class);

    private final ReviewSchedulingAgent schedulingAgent;
    private final ContentGenerationAgent contentAgent;
    private final UserWordRepository userWordRepository;
    private final WordRepository wordRepository;
    private final LearningSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ReviewRecordRepository reviewRecordRepository;

    public LearningController(ReviewSchedulingAgent schedulingAgent, ContentGenerationAgent contentAgent,
                              UserWordRepository userWordRepository, WordRepository wordRepository,
                              LearningSessionRepository sessionRepository, UserRepository userRepository,
                              ReviewRecordRepository reviewRecordRepository) {
        this.schedulingAgent = schedulingAgent;
        this.contentAgent = contentAgent;
        this.userWordRepository = userWordRepository;
        this.wordRepository = wordRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.reviewRecordRepository = reviewRecordRepository;
    }

    @PostMapping("/start-session")
    public ResponseEntity<?> startSession(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String type = body.getOrDefault("sessionType", "new_words");
        LearningSession session = LearningSession.builder()
                .userId(user.getId())
                .sessionType(type)
                .build();
        sessionRepository.save(session);
        return ResponseEntity.ok(Map.of("sessionId", session.getId(), "sessionType", session.getSessionType()));
    }

    @GetMapping("/new-words")
    public ResponseEntity<?> getNewWords(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "false") boolean random) {
        List<Map<String, Object>> words = schedulingAgent.getNewWords(user, count, random);
        return ResponseEntity.ok(Map.of("words", words, "count", words.size(), "random", random));
    }

    @GetMapping("/due-words")
    public ResponseEntity<?> getDueWords(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "20") int limit) {
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        List<String> reviewStatuses = List.of("learning", "review", "new");
        
        List<UserWord> userWords = userWordRepository.findDueWords(user.getId(), todayEnd, 
                org.springframework.data.domain.PageRequest.of(0, limit));
        
        System.out.println("=== DEBUG Due Words API ===");
        System.out.println("User ID: " + user.getId());
        System.out.println("Today End: " + todayEnd);
        System.out.println("Review Statuses: " + reviewStatuses);
        System.out.println("Found due words count: " + userWords.size());
        
        List<Map<String, Object>> words = new ArrayList<>();
        for (UserWord uw : userWords) {
            System.out.println("Word ID: " + uw.getWordId() + ", Status: " + uw.getStatus() + 
                    ", NextReview: " + uw.getNextReview() + ", LearningQueue: " + uw.getLearningQueue());
            
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userWordId", uw.getId());
            item.put("wordId", uw.getWordId());
            item.put("status", uw.getStatus());
            item.put("nextReview", uw.getNextReview() != null ? uw.getNextReview().toString() : null);
            if (uw.getWord() != null) {
                Word w = uw.getWord();
                item.put("word", w.getWord());
                item.put("phonetic", w.getPhonetic());
                item.put("definition_cn", w.getDefinitionCn());
                item.put("definition_en", w.getDefinitionEn());
            }
            words.add(item);
        }
        
        long totalCount = userWordRepository.countByUserIdAndNextReviewBeforeAndStatusIn(user.getId(), todayEnd, reviewStatuses);
        System.out.println("Total due count: " + totalCount);
        
        return ResponseEntity.ok(Map.of("words", words, "count", totalCount));
    }

    @PostMapping("/submit-review")
    public ResponseEntity<?> submitReview(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        Object sessionIdObj = body.get("sessionId");
        if (sessionIdObj == null) {
            return ResponseEntity.badRequest().body(Map.of("detail", "sessionId is required"));
        }
        Long sessionId = Long.valueOf(sessionIdObj.toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reviews = (List<Map<String, Object>>) body.get("reviews");
        if (reviews == null) {
            return ResponseEntity.badRequest().body(Map.of("detail", "reviews list is required"));
        }

        List<Map<String, Object>> results = new ArrayList<>();
        int correctCount = 0;

        for (Map<String, Object> r : reviews) {
            Object uwIdObj = r.get("userWordId");
            Object qualityObj = r.get("quality");
            if (uwIdObj == null || qualityObj == null) continue;
            Long uwId = Long.valueOf(uwIdObj.toString());
            int quality = Integer.parseInt(qualityObj.toString());

            UserWord uw = userWordRepository.findById(uwId).orElse(null);
            if (uw == null) continue;

            Map<String, Object> result = schedulingAgent.scheduleReview(user, uw, quality);
            results.add(result);
            if (quality >= 3) correctCount++;
        }

        // Update session
        LearningSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session != null) {
            session.setWordsStudied(results.size());
            session.setWordsCorrect(correctCount);
            session.setEndedAt(LocalDateTime.now());
            session.setSessionScore(results.isEmpty() ? 0.0 : (double) correctCount / results.size());
            sessionRepository.save(session);
        }

        // Update streak
        updateStreak(user);

        return ResponseEntity.ok(Map.of("results", results, "total", results.size()));
    }

    @PostMapping("/learn-word/{wordId}")
    public ResponseEntity<?> learnWord(
            @AuthenticationPrincipal User user,
            @PathVariable Long wordId) {
        Word word = wordRepository.findById(wordId).orElse(null);
        if (word == null) {
            return ResponseEntity.status(404).body(Map.of("detail", "Word not found"));
        }

        Optional<UserWord> existing = userWordRepository.findByUserIdAndWordId(user.getId(), wordId);
        if (existing.isPresent()) {
            return ResponseEntity.ok(Map.of("userWordId", existing.get().getId(), "status", existing.get().getStatus()));
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReviewTime = now;
        UserWord uw = UserWord.builder()
                .userId(user.getId())
                .wordId(wordId)
                .status("learning")
                .masteryLevel(0.1)
                .learningQueue("new")
                .learnDate(now)
                .firstLearned(now)
                .lastReviewed(now)
                .nextReview(nextReviewTime)
                .repetitions(0)
                .intervalDays(0)
                .build();
        userWordRepository.save(uw);
        
        LearningSession session = LearningSession.builder()
                .userId(user.getId())
                .sessionType("new_words")
                .wordsStudied(1)
                .startedAt(LocalDateTime.now())
                .build();
        sessionRepository.save(session);
        
        updateStreak(user);
        
        return ResponseEntity.ok(Map.of("userWordId", uw.getId(), "status", uw.getStatus(), "streakDays", user.getStreakDays()));
    }

    @GetMapping("/word-card/{wordId}")
    public ResponseEntity<?> getWordCard(
            @AuthenticationPrincipal User user,
            @PathVariable Long wordId) {
        Word word = wordRepository.findById(wordId).orElse(null);
        if (word == null) {
            return ResponseEntity.status(404).body(Map.of("detail", "Word not found"));
        }
        Map<String, Object> content = contentAgent.generateCard(user, word);
        return ResponseEntity.ok(Map.of("word", word.getWord(), "content", content));
    }

    @GetMapping("/word-image/{wordId}")
    public ResponseEntity<?> getWordImage(
            @AuthenticationPrincipal User user,
            @PathVariable Long wordId,
            @RequestParam(defaultValue = "1024x1024") String imageSize) {
        Word word = wordRepository.findById(wordId).orElse(null);
        if (word == null) {
            return ResponseEntity.status(404).body(Map.of("detail", "Word not found"));
        }
        Map<String, Object> imageResult = contentAgent.generateWordImage(user, word, imageSize);
        return ResponseEntity.ok(Map.of("word", word.getWord(), "imageSize", imageSize, "result", imageResult));
    }

    @GetMapping("/word-image-by-text")
    public ResponseEntity<?> getWordImageByText(
            @AuthenticationPrincipal User user,
            @RequestParam String wordText,
            @RequestParam(defaultValue = "1024x1024") String imageSize) {
        Word word = wordRepository.findByWord(wordText.toLowerCase()).orElse(null);
        if (word == null) {
            return ResponseEntity.ok(Map.of("status", "error", "message", "Word not found in database"));
        }
        Map<String, Object> imageResult = contentAgent.generateWordImage(user, word, imageSize);
        return ResponseEntity.ok(Map.of("word", word.getWord(), "imageSize", imageSize, "result", imageResult));
    }

    @GetMapping("/video-script/{wordId}")
    public ResponseEntity<?> getVideoScript(
            @AuthenticationPrincipal User user,
            @PathVariable Long wordId) {
        return ResponseEntity.status(410).body(Map.of("detail", "Video script generation feature has been removed"));
    }

    @GetMapping("/learning-package/{wordId}")
    public ResponseEntity<?> getLearningPackage(
            @AuthenticationPrincipal User user,
            @PathVariable Long wordId) {
        Word word = wordRepository.findById(wordId).orElse(null);
        if (word == null) {
            return ResponseEntity.status(404).body(Map.of("detail", "Word not found"));
        }
        Map<String, Object> learningPackage = contentAgent.generateLearningPackage(user, word);
        return ResponseEntity.ok(learningPackage);
    }

    @GetMapping("/personalized-card/{wordId}")
    public ResponseEntity<?> getPersonalizedCard(
            @AuthenticationPrincipal User user,
            @PathVariable Long wordId) {
        Word word = wordRepository.findById(wordId).orElse(null);
        if (word == null) {
            return ResponseEntity.status(404).body(Map.of("detail", "Word not found"));
        }
        Map<String, Object> diagnosisData = new LinkedHashMap<>();
        diagnosisData.put("memoryStyle", Map.of("style", user.getMemoryStyle()));
        diagnosisData.put("memoryStrength", user.getMemoryStrength());
        
        Map<String, Object> card = contentAgent.generatePersonalizedCard(user, word, diagnosisData);
        return ResponseEntity.ok(Map.of("word", word.getWord(), "card", card));
    }

    @GetMapping("/image-prompt/{wordId}")
    public ResponseEntity<?> getImagePrompt(
            @AuthenticationPrincipal User user,
            @PathVariable Long wordId) {
        Word word = wordRepository.findById(wordId).orElse(null);
        if (word == null) {
            return ResponseEntity.status(404).body(Map.of("detail", "Word not found"));
        }
        String prompt = contentAgent.generateImagePrompt(word, user);
        return ResponseEntity.ok(Map.of("word", word.getWord(), "prompt", prompt));
    }

    @GetMapping("/schedule-overview")
    public ResponseEntity<?> getSchedule(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(schedulingAgent.getScheduleOverview(user));
    }

    @PostMapping("/quiz")
    public ResponseEntity<?> generateQuiz(@AuthenticationPrincipal User user) {
        List<UserWord> recent = userWordRepository.findRecentWords(
                user.getId(), org.springframework.data.domain.PageRequest.of(0, 10));
        Set<Long> wordIds = new HashSet<>();
        recent.forEach(uw -> wordIds.add(uw.getWordId()));
        List<Word> words = wordRepository.findAllById(wordIds);
        Map<String, Object> quiz = contentAgent.generateQuiz(user, words);
        return ResponseEntity.ok(quiz);
    }

    private void updateStreak(User user) {
        // Full recalculation from sessions + UserWord dates — always accurate, no edge cases
        LocalDate today = LocalDate.now();
        Set<LocalDate> studyDates = new HashSet<>();

        // Source 1: LearningSession dates
        List<LearningSession> sessions = sessionRepository.findRecentSessions(user.getId(),
                org.springframework.data.domain.PageRequest.of(0, 200));
        for (LearningSession s : sessions) {
            if (s.getStartedAt() != null) {
                studyDates.add(s.getStartedAt().toLocalDate());
            }
        }

        // Source 2: UserWord.firstLearned dates (for days with new words but no sessions)
        LocalDateTime since = today.minusDays(365).atStartOfDay();
        List<Object[]> newWordsByDate = userWordRepository.countNewWordsByDate(user.getId(), since);
        for (Object[] row : newWordsByDate) {
            try {
                studyDates.add(LocalDate.parse(row[0].toString()));
            } catch (Exception ignored) {}
        }

        // Walk backward from today to compute current streak
        int currentStreak = 0;
        for (int i = 0; i < 365; i++) {
            LocalDate checkDate = today.minusDays(i);
            if (studyDates.contains(checkDate)) {
                currentStreak++;
            } else if (i > 0) {
                break;
            }
        }

        user.setStreakDays(currentStreak);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    }

    @GetMapping("/review-history")
    public ResponseEntity<?> getReviewHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "20") int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<ReviewRecord> records = reviewRecordRepository.findRecentReviews(
                user.getId(), org.springframework.data.domain.PageRequest.of(0, limit));

        List<Map<String, Object>> history = new ArrayList<>();
        for (ReviewRecord r : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("wordId", r.getWordId());
            item.put("userWordId", r.getUserWordId());
            item.put("quality", r.getQuality());
            item.put("correct", r.getCorrect());
            item.put("reviewType", r.getReviewType());
            item.put("responseTime", r.getResponseTime());
            item.put("prevMastery", r.getPrevMastery());
            item.put("newMastery", r.getNewMastery());
            item.put("prevInterval", r.getPrevInterval());
            item.put("newInterval", r.getNewInterval());
            item.put("reviewedAt", r.getReviewedAt().toString());

            Word word = wordRepository.findById(r.getWordId()).orElse(null);
            if (word != null) {
                item.put("word", word.getWord());
                item.put("definitionCn", word.getDefinitionCn());
            }
            history.add(item);
        }

        return ResponseEntity.ok(Map.of("history", history, "total", history.size()));
    }

    @GetMapping("/review-stats")
    public ResponseEntity<?> getReviewStats(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "7") int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        long totalReviews = reviewRecordRepository.countByUserIdSince(user.getId(), since);
        long correctReviews = reviewRecordRepository.countCorrectByUserIdSince(user.getId(), since);
        Double avgQuality = reviewRecordRepository.getAvgQualitySince(user.getId(), since);

        List<Object[]> dailyStats = reviewRecordRepository.getDailyReviewStats(user.getId(), since);
        List<Map<String, Object>> daily = new ArrayList<>();
        for (Object[] row : dailyStats) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", row[0] != null ? row[0].toString() : "");
            day.put("total", row[1] != null ? ((Number) row[1]).longValue() : 0);
            day.put("correct", row[2] != null ? ((Number) row[2]).longValue() : 0);
            daily.add(day);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalReviews", totalReviews);
        result.put("correctReviews", correctReviews);
        result.put("accuracy", totalReviews > 0 ? Math.round((double) correctReviews / totalReviews * 1000.0) / 10.0 : 0.0);
        result.put("avgQuality", avgQuality != null ? Math.round(avgQuality * 100.0) / 100.0 : 0.0);
        result.put("dailyStats", daily);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset-progress")
    @Transactional
    public ResponseEntity<?> resetProgress(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "false") boolean includeMastered,
            @RequestParam(defaultValue = "false") boolean fullReset) {
        try {
            int resetCount;

            if (fullReset) {
                // 完全重置：删除所有学习记录
                List<UserWord> userWords = userWordRepository.findByUserId(user.getId());
                resetCount = userWords.size();
                userWordRepository.deleteByUserId(user.getId());

                // 清空词库选择，让用户重新选择
                user.setSelectedLibrary(null);
                user.setStreakDays(0);
                user.setAvgRecallRate(0.0);
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
            } else {
                // 部分重置：仅重置状态
                List<UserWord> userWords = userWordRepository.findByUserId(user.getId());
                log.info("Resetting progress for user {} with {} words, includeMastered={}", user.getId(), userWords.size(), includeMastered);

                resetCount = 0;
                for (UserWord uw : userWords) {
                    if (includeMastered || !"mastered".equals(uw.getStatus())) {
                        uw.setStatus("new");
                        uw.setLearningQueue("new");
                        uw.setIntervalDays(0);
                        uw.setRepetitions(0);
                        uw.setMasteryLevel(0.0);
                        uw.setFamiliarity(0.0);
                        resetCount++;
                    }
                }
                userWordRepository.saveAll(userWords);
            }

            log.info("Reset progress for user {}, resetCount={}, fullReset={}", user.getId(), resetCount, fullReset);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", fullReset ? "学习记录已清空，请重新选择词库" : "学习进度已重置");
            result.put("resetCount", resetCount);
            result.put("fullReset", fullReset);
            result.put("needSelectLibrary", fullReset);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to reset progress for user {}: {}", user.getId(), e.getMessage(), e);
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("message", "重置失败：" + e.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }

    @GetMapping("/all-words-for-review")
    public ResponseEntity<?> getAllWordsForReview(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "100") int limit) {
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        List<UserWord> userWords = userWordRepository.findDueWords(user.getId(), todayEnd, 
                org.springframework.data.domain.PageRequest.of(0, limit));
        
        List<Map<String, Object>> words = new ArrayList<>();
        int count = 0;
        for (UserWord uw : userWords) {
            if (count >= limit) break;

            Word word = wordRepository.findById(uw.getWordId()).orElse(null);
            if (word == null) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userWordId", uw.getId());
            item.put("wordId", uw.getWordId());
            item.put("word", word.getWord());
            item.put("phonetic", word.getPhonetic());
            item.put("definitionCn", word.getDefinitionCn());
            item.put("definitionEn", word.getDefinitionEn());
            item.put("difficulty", word.getDifficulty());
            item.put("masteryLevel", uw.getMasteryLevel());
            item.put("status", uw.getStatus());
            item.put("intervalDays", uw.getIntervalDays());
            item.put("nextReview", uw.getNextReview() != null ? uw.getNextReview().toString() : null);
            item.put("reviewCount", uw.getReviewCount());
            item.put("correctCount", uw.getCorrectCount());
            item.put("wrongCount", uw.getWrongCount());
            item.put("lastReviewed", uw.getLastReviewed() != null ? uw.getLastReviewed().toString() : null);

            words.add(item);
            count++;
        }

        return ResponseEntity.ok(Map.of("words", words, "total", words.size()));
    }

    @GetMapping("/learned-words")
    public ResponseEntity<?> getAllLearnedWords(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "200") int limit) {
        List<UserWord> userWords = userWordRepository.findByUserId(user.getId());
        
        List<Map<String, Object>> words = new ArrayList<>();
        int count = 0;
        for (UserWord uw : userWords) {
            if (count >= limit) break;

            Word word = wordRepository.findById(uw.getWordId()).orElse(null);
            if (word == null) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userWordId", uw.getId());
            item.put("wordId", uw.getWordId());
            item.put("word", word.getWord());
            item.put("phonetic", word.getPhonetic());
            item.put("definitionCn", word.getDefinitionCn());
            item.put("definitionEn", word.getDefinitionEn());
            item.put("difficulty", word.getDifficulty());
            item.put("masteryLevel", uw.getMasteryLevel());
            item.put("status", uw.getStatus());
            item.put("intervalDays", uw.getIntervalDays());
            item.put("nextReview", uw.getNextReview() != null ? uw.getNextReview().toString() : null);
            item.put("reviewCount", uw.getReviewCount());
            item.put("correctCount", uw.getCorrectCount());
            item.put("wrongCount", uw.getWrongCount());
            item.put("lastReviewed", uw.getLastReviewed() != null ? uw.getLastReviewed().toString() : null);

            words.add(item);
            count++;
        }

        return ResponseEntity.ok(Map.of("words", words, "total", words.size()));
    }
}
