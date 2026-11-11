package com.vocab.agent;

import com.vocab.entity.LearningSession;
import com.vocab.entity.ReviewRecord;
import com.vocab.entity.User;
import com.vocab.entity.UserWord;
import com.vocab.entity.Word;
import com.vocab.repository.LearningSessionRepository;
import com.vocab.repository.ReviewRecordRepository;
import com.vocab.repository.UserWordRepository;
import com.vocab.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Review Scheduling Agent - implements a modified SM-2 spaced repetition
 * algorithm to schedule vocabulary reviews, select due words, recommend
 * new words, and provide LLM-powered learning recommendations.
 * 
 * Enhanced features:
 * - Optimized SM-2 algorithm with personalized memory factor adjustments
 * - Intelligent word grouping strategies (by difficulty, category, topic)
 * - Adaptive scheduling based on user's optimal study times and memory diagnosis
 * - Dynamic difficulty adjustment based on learning performance
 * - Advanced prioritization with time-slot based scheduling
 */
@Component
public class ReviewSchedulingAgent extends BaseAgent {

    @Autowired
    private UserWordRepository userWordRepository;

    @Autowired
    private ReviewRecordRepository reviewRecordRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private LearningSessionRepository learningSessionRepository;

    /** Priority weights for different word grouping strategies */
    private static final double[] GROUP_PRIORITY_WEIGHTS = {
        0.3, // difficulty group
        0.25, // topic group
        0.2, // frequency group
        0.15, // mastery group
        0.1 // error rate group
    };

    /** Time slot weights for optimal scheduling */
    private static final int[] OPTIMAL_HOURS = {9, 10, 11, 14, 15, 16, 19, 20, 21};

    // =========================================================================
    // Enhanced SM-2 with Personalized Adjustments
    // =========================================================================

    /**
     * Schedule the next review for a word using the modified SM-2 algorithm.
     *
     * Quality scale (0-5):
     *   5 = perfect response
     *   4 = correct with slight hesitation
     *   3 = correct with serious difficulty
     *   2 = incorrect, but the answer seemed easy to recall
     *   1 = incorrect, the answer was hard to remember
     *   0 = complete blackout, total failure
     *
     * @param user     the user performing the review
     * @param userWord the user-word record being reviewed
     * @param quality  SM-2 quality rating (0-5)
     * @return a map containing scheduling results: wordId, quality, newInterval,
     *         newEaseFactor, nextReview, masteryLevel, status
     */
    public Map<String, Object> scheduleReview(User user, UserWord userWord, int quality) {
        log.info("Scheduling review for user={}, word={}, quality={}",
                user.getUsername(), userWord.getWordId(), quality);

        // Clamp quality to valid range
        quality = Math.max(0, Math.min(5, quality));

        // Capture previous state for the review record
        double prevMastery = userWord.getMasteryLevel() != null ? userWord.getMasteryLevel() : 0.0;
        int prevInterval = userWord.getIntervalDays() != null ? userWord.getIntervalDays() : 0;
        double prevEaseFactor = userWord.getEaseFactor() != null ? userWord.getEaseFactor() : 2.5;

        // --- SM-2 Ease Factor adjustment ---
        double ef = prevEaseFactor;
        double memoryFactor = user.getMemoryStrength() != null ? user.getMemoryStrength() : 1.0;
        double efAdjustment = 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02) * memoryFactor;
        ef = ef + efAdjustment;

        // Enforce minimum EF
        double minEf = llmConfig.getMinEf();
        ef = Math.max(minEf, ef);

        // --- Interval calculation ---
        int reps = userWord.getRepetitions() != null ? userWord.getRepetitions() : 0;
        int interval = prevInterval;

        if (quality < 3) {
            // Failed recall: reset repetitions, shrink interval
            reps = 0;
            interval = 1;
            log.debug("Failed recall (q={}): reps reset, interval reset to {}", quality, interval);
        } else {
            // Successful recall - periodic review schedule
            if (reps == 0) {
                // First review: today (day 0)
                interval = 0;
            } else if (reps == 1) {
                // Second review: tomorrow (day 1)
                interval = 1;
            } else if (reps == 2) {
                // Third review: 3 days later
                interval = 3;
            } else {
                // Fourth and beyond: weekly interval (7 days per cycle)
                interval = 7 * (reps - 2);
            }
            reps++;
            log.debug("Successful recall (q={}): reps={}, interval={}", quality, reps, interval);
        }

        // --- Apply personalized memory factor ---
        double memoryStrength = user.getMemoryStrength() != null ? user.getMemoryStrength() : 1.0;
        interval = Math.max(1, (int) Math.round(interval * (0.8 + 0.4 * memoryStrength)));

        // --- Update mastery level ---
        double mastery = prevMastery;
        if (quality >= 3) {
            mastery = mastery + 0.1 * (quality / 5.0);
        } else {
            mastery = mastery - 0.15;
        }
        mastery = Math.max(0.0, Math.min(1.0, mastery));

        // --- Update memory stability and retrievability ---
        double stability = interval * 0.9;
        double retrievability = 1.0; // Freshly reviewed, retrievability resets to max

        // --- Update status ---
        String status;
        if (mastery >= 0.85 && reps >= 3) {
            status = "mastered";
        } else if (reps > 0) {
            status = "review";
        } else {
            status = "learning";
        }

        // --- Compute next review date ---
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReview = now.plusDays(interval);

        // --- Update review statistics ---
        int reviewCount = (userWord.getReviewCount() != null ? userWord.getReviewCount() : 0) + 1;
        int correctCount = userWord.getCorrectCount() != null ? userWord.getCorrectCount() : 0;
        int wrongCount = userWord.getWrongCount() != null ? userWord.getWrongCount() : 0;
        if (quality >= 3) {
            correctCount++;
        } else {
            wrongCount++;
        }

        // --- Persist changes to UserWord ---
        userWord.setEaseFactor(ef);
        userWord.setIntervalDays(interval);
        userWord.setRepetitions(reps);
        userWord.setMasteryLevel(mastery);
        userWord.setMemoryStability(stability);
        userWord.setMemoryRetrievability(retrievability);
        userWord.setLastReviewed(now);
        userWord.setNextReview(nextReview);
        userWord.setStatus(status);
        userWord.setLearningQueue("review");
        userWord.setReviewCount(reviewCount);
        userWord.setCorrectCount(correctCount);
        userWord.setWrongCount(wrongCount);
        userWordRepository.save(userWord);

        // --- Create and save ReviewRecord ---
        ReviewRecord record = ReviewRecord.builder()
                .userId(user.getId())
                .wordId(userWord.getWordId())
                .userWordId(userWord.getId())
                .quality(quality)
                .correct(quality >= 3)
                .reviewType("recall")
                .prevMastery(prevMastery)
                .prevInterval(prevInterval)
                .prevEaseFactor(prevEaseFactor)
                .newMastery(mastery)
                .newInterval(interval)
                .newEaseFactor(ef)
                .reviewedAt(now)
                .build();
        reviewRecordRepository.save(record);

        log.info("Review scheduled: word={}, interval={} days, EF={}, mastery={}, status={}, nextReview={}",
                userWord.getWordId(), interval, String.format("%.2f", ef),
                String.format("%.2f", mastery), status, nextReview);

        // --- Build result map ---
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("wordId", userWord.getWordId());
        result.put("quality", quality);
        result.put("newInterval", interval);
        result.put("newEaseFactor", Math.round(ef * 100.0) / 100.0);
        result.put("nextReview", nextReview.toString());
        result.put("masteryLevel", Math.round(mastery * 10000.0) / 10000.0);
        result.put("status", status);
        return result;
    }

    // =========================================================================
    // Due Word Selection
    // =========================================================================

    /**
     * Retrieve words that are due for review, prioritized by urgency, mastery
     * deficit, memory retrievability, difficulty, and historical error rate.
     *
     * Also includes "declining" words whose retrievability has dropped below 0.5
     * even if they are not yet overdue.
     *
     * @param user  the user
     * @param limit maximum number of words to return
     * @return a prioritized list of due-word data maps
     */
    public List<Map<String, Object>> getDueWords(User user, int limit) {
        log.info("Fetching due words for user={}, limit={}", user.getUsername(), limit);

        LocalDateTime now = LocalDateTime.now();
        PageRequest pageRequest = PageRequest.of(0, limit);

        List<UserWord> allCandidates = userWordRepository.findDueWords(user.getId(), now, pageRequest);
        log.debug("Found {} due words", allCandidates.size());

        // Calculate priority score for each candidate
        List<Map.Entry<UserWord, Double>> scored = new ArrayList<>();
        for (UserWord uw : allCandidates) {
            double priority = calculatePriority(uw, now);
            scored.add(Map.entry(uw, priority));
        }

        // Sort by priority descending
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Build result list up to limit
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, scored.size()); i++) {
            Map.Entry<UserWord, Double> entry = scored.get(i);
            UserWord uw = entry.getKey();
            double priority = entry.getValue();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userWordId", uw.getId());
            item.put("wordId", uw.getWordId());
            item.put("masteryLevel", uw.getMasteryLevel());
            item.put("memoryRetrievability", uw.getMemoryRetrievability());
            item.put("easeFactor", uw.getEaseFactor());
            item.put("intervalDays", uw.getIntervalDays());
            item.put("repetitions", uw.getRepetitions());
            item.put("nextReview", uw.getNextReview() != null ? uw.getNextReview().toString() : null);
            item.put("status", uw.getStatus());
            item.put("priority", Math.round(priority * 100.0) / 100.0);

            // Compute overdue hours
            if (uw.getNextReview() != null && uw.getNextReview().isBefore(now)) {
                long overdueHours = ChronoUnit.HOURS.between(uw.getNextReview(), now);
                item.put("overdueHours", overdueHours);
            } else {
                item.put("overdueHours", 0L);
            }

            // Attempt to load word text and definitions for display convenience
            try {
                if (uw.getWord() != null) {
                    Word w = uw.getWord();
                    item.put("word", w.getWord());
                    item.put("phonetic", w.getPhonetic());
                    item.put("definition_cn", w.getDefinitionCn());
                    item.put("definition_en", w.getDefinitionEn());
                    item.put("difficulty", w.getDifficulty());
                }
            } catch (Exception e) {
                log.debug("Could not lazy-load word for userWord {}: {}", uw.getId(), e.getMessage());
            }

            result.add(item);
        }

        log.info("Returning {} due words for user={}", result.size(), user.getUsername());
        return result;
    }

    /**
     * Calculate a priority score for a word based on multiple learning factors.
     *
     * Formula:
     *   priority = overdue_hours * 2
     *            + (1 - mastery) * 20
     *            + (1 - retrievability) * 15
     *            + difficulty * 2
     *            + error_rate * 10
     */
    private double calculatePriority(UserWord uw, LocalDateTime now) {
        double priority = 0.0;

        // Overdue hours component
        if (uw.getNextReview() != null && uw.getNextReview().isBefore(now)) {
            long overdueHours = ChronoUnit.HOURS.between(uw.getNextReview(), now);
            priority += overdueHours * 2.0;
        }

        // Mastery deficit
        double mastery = uw.getMasteryLevel() != null ? uw.getMasteryLevel() : 0.0;
        priority += (1.0 - mastery) * 20.0;

        // Retrievability deficit
        double retrievability = uw.getMemoryRetrievability() != null ? uw.getMemoryRetrievability() : 1.0;
        priority += (1.0 - retrievability) * 15.0;

        // Word difficulty (attempt to load from word entity)
        double difficulty = 3.0; // default
        try {
            if (uw.getWord() != null && uw.getWord().getDifficulty() != null) {
                difficulty = uw.getWord().getDifficulty();
            }
        } catch (Exception e) {
            // Lazy loading failure; use default
        }
        priority += difficulty * 2.0;

        // Historical error rate
        int reviewCount = uw.getReviewCount() != null ? uw.getReviewCount() : 0;
        int wrongCount = uw.getWrongCount() != null ? uw.getWrongCount() : 0;
        double errorRate = reviewCount > 0 ? (double) wrongCount / reviewCount : 0.0;
        priority += errorRate * 10.0;

        return priority;
    }

    // =========================================================================
    // New Word Selection
    // =========================================================================

    /**
     * Select new words for the user to learn, matched to their current
     * difficulty level.
     *
     * Words already learned by the user are excluded. The difficulty range
     * is [max(1, level-1), level+1] where level is the user's difficultyLevel.
     *
     * @param user  the user
     * @param count maximum number of new words to return
     * @return a list of word data maps
     */
    public List<Map<String, Object>> getNewWords(User user, int count) {
        return getNewWords(user, count, false);
    }

    public List<Map<String, Object>> getNewWords(User user, int count, boolean random) {
        log.info("Fetching new words for user={}, count={}, random={}", user.getUsername(), count, random);

        // Get IDs of ALL words the user has interacted with (not just mastered)
        Set<Long> learnedIds = userWordRepository.findLearnedWordIds(user.getId());
        log.debug("User {} has learned {} words (all statuses)", user.getUsername(), learnedIds.size());

        List<Word> newWords;

        // If user has a selected library, filter by tag
        String library = user.getSelectedLibrary();
        if (library != null && !library.isEmpty()) {
            if (random) {
                List<Word> candidates = wordRepository.findNewWordsByTag(learnedIds, library, PageRequest.of(0, count * 3));
                Collections.shuffle(candidates);
                newWords = candidates.stream().limit(count).toList();
            } else {
                newWords = wordRepository.findNewWordsByTag(learnedIds, library, PageRequest.of(0, count));
            }
            log.debug("Found {} new words for library tag '{}'", newWords.size(), library);
        } else {
            // Fallback: use difficulty-based filtering
            int level = user.getDifficultyLevel() != null ? user.getDifficultyLevel() : 3;
            int minDiff = Math.max(1, level - 1);
            int maxDiff = level + 1;
            if (random) {
                List<Word> candidates = wordRepository.findNewWords(learnedIds, minDiff, maxDiff, PageRequest.of(0, count * 3));
                Collections.shuffle(candidates);
                newWords = candidates.stream().limit(count).toList();
            } else {
                newWords = wordRepository.findNewWords(learnedIds, minDiff, maxDiff, PageRequest.of(0, count));
            }
            log.debug("Found {} new words in difficulty range [{}, {}]", newWords.size(), minDiff, maxDiff);
        }

        // Build result maps
        List<Map<String, Object>> result = new ArrayList<>();
        for (Word word : newWords) {
            Map<String, Object> wordMap = new LinkedHashMap<>();
            wordMap.put("wordId", word.getId());
            wordMap.put("word", word.getWord());
            wordMap.put("phonetic", word.getPhonetic());
            wordMap.put("partOfSpeech", word.getPartOfSpeech());
            wordMap.put("definitionEn", word.getDefinitionEn());
            wordMap.put("definitionCn", word.getDefinitionCn());
            wordMap.put("difficulty", word.getDifficulty());
            wordMap.put("frequencyRank", word.getFrequencyRank());
            wordMap.put("tags", word.getTags());
            wordMap.put("exampleSentences", word.getExampleSentences());
            wordMap.put("etymology", word.getEtymology());
            wordMap.put("synonyms", word.getSynonyms());
            wordMap.put("antonyms", word.getAntonyms());
            result.add(wordMap);
        }

        log.info("Returning {} new words for user={}", result.size(), user.getUsername());
        return result;
    }

    // =========================================================================
    // Schedule Overview
    // =========================================================================

    /**
     * Build a 7-day schedule overview showing the number of words due for
     * review on each day, plus aggregate counts for overdue and today.
     *
     * @param user the user
     * @return a map containing "schedule" (list of day maps), "overdueCount",
     *         and "totalDueToday"
     */
    public Map<String, Object> getScheduleOverview(User user) {
        log.info("Building schedule overview for user={}", user.getUsername());

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
        List<String> reviewStatuses = List.of("learning", "review", "new");

        long overdueCount = userWordRepository.countByUserIdAndNextReviewBeforeAndStatusIn(
                user.getId(), todayEnd, reviewStatuses);

        // Build day-by-day schedule for the next 7 days
        List<Map<String, Object>> schedule = new ArrayList<>();
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate day = today.plusDays(dayOffset);
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.atTime(LocalTime.MAX);

            long count = userWordRepository.countByDateRange(user.getId(), dayStart, dayEnd);
            
            // Count completed reviews for this day
            long completedCount = 0;
            if (!day.isAfter(today)) {
                completedCount = reviewRecordRepository.countByUserIdAndReviewedAtBetween(
                        user.getId(), dayStart, dayEnd);
            }

            // Count review cycles for this day
            Map<Integer, Long> cycleCounts = new LinkedHashMap<>();
            if (count > 0) {
                List<Object[]> cycleData = userWordRepository.countByRepetitionCycle(user.getId(), dayStart, dayEnd);
                for (Object[] row : cycleData) {
                    Integer reps = row[0] != null ? ((Number) row[0]).intValue() : 0;
                    Long cnt = row[1] != null ? ((Number) row[1]).longValue() : 0;
                    cycleCounts.put(reps + 1, cnt);
                }
            }

            Map<String, Object> dayMap = new LinkedHashMap<>();
            dayMap.put("date", day.toString());
            dayMap.put("dayOfWeek", day.getDayOfWeek().toString());
            dayMap.put("dueCount", count);
            dayMap.put("completedCount", completedCount);
            dayMap.put("isToday", dayOffset == 0);
            dayMap.put("cycleCounts", cycleCounts);
            schedule.add(dayMap);
        }

        // Total due today = overdue count (already expired) + today's scheduled count
        long totalDueToday = overdueCount;
        if (!schedule.isEmpty()) {
            Object dueCountObj = schedule.get(0).get("dueCount");
            long todayCount = dueCountObj instanceof Number ? ((Number) dueCountObj).longValue() : 0;
            totalDueToday += todayCount;
        }

        // Update today's entry to show the total due (overdue + today scheduled)
        if (!schedule.isEmpty()) {
            schedule.get(0).put("dueCount", totalDueToday);
            schedule.get(0).put("overdueCount", overdueCount);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schedule", schedule);
        result.put("overdueCount", overdueCount);
        result.put("totalDueToday", totalDueToday);

        log.info("Schedule overview for user={}: overdue={}, dueToday={}",
                user.getUsername(), overdueCount, totalDueToday);
        return result;
    }

    // =========================================================================
    // LLM-Powered Recommendations
    // =========================================================================

    /**
     * Generate personalized learning recommendations using LLM analysis of the
     * user's review history, mastery distribution, and scheduling data.
     * Falls back to rule-based recommendations when LLM is unavailable.
     *
     * @param user the user
     * @return a map containing "recommendations" (list of strings), "analysis"
     *         (summary text), and "source" ("llm" or "fallback")
     */
    public Map<String, Object> getRecommendations(User user) {
        log.info("Generating recommendations for user={}", user.getUsername());

        // Gather user learning statistics
        List<UserWord> userWords = userWordRepository.findByUserId(user.getId());
        List<ReviewRecord> reviews = reviewRecordRepository.findByUserIdOrderByReviewedAtDesc(user.getId());

        long totalLearned = userWords.size();
        long masteredCount = userWords.stream().filter(uw -> "mastered".equals(uw.getStatus())).count();
        long reviewCount = userWords.stream().filter(uw -> "review".equals(uw.getStatus())).count();
        long learningCount = userWords.stream().filter(uw -> "learning".equals(uw.getStatus())).count();

        double avgMastery = userWords.stream()
                .mapToDouble(uw -> uw.getMasteryLevel() != null ? uw.getMasteryLevel() : 0.0)
                .average()
                .orElse(0.0);

        double avgRetrievability = userWords.stream()
                .mapToDouble(uw -> uw.getMemoryRetrievability() != null ? uw.getMemoryRetrievability() : 0.0)
                .average()
                .orElse(0.0);

        // Recent review accuracy (last 50 reviews)
        int recentWindow = Math.min(reviews.size(), 50);
        long recentCorrect = reviews.stream()
                .limit(recentWindow)
                .filter(r -> Boolean.TRUE.equals(r.getCorrect()))
                .count();
        double recentAccuracy = recentWindow > 0 ? (double) recentCorrect / recentWindow : 0.0;

        // Overdue word count
        LocalDateTime now = LocalDateTime.now();
        long overdueCount = userWords.stream()
                .filter(uw -> uw.getNextReview() != null && uw.getNextReview().isBefore(now))
                .filter(uw -> !"mastered".equals(uw.getStatus()))
                .count();

        // Build statistics summary for the prompt
        String statsSummary = String.format(
            "User: %s\n" +
            "Target exam: %s\n" +
            "Target vocab size: %d\n" +
            "Daily new words setting: %d\n" +
            "Difficulty level: %d\n" +
            "Memory strength: %.2f\n" +
            "Average recall rate: %.2f\n" +
            "Streak days: %d\n\n" +
            "Total words learned: %d\n" +
            "Mastered: %d, Review: %d, Learning: %d\n" +
            "Average mastery: %.2f\n" +
            "Average retrievability: %.2f\n" +
            "Recent review accuracy (last %d): %.1f%%\n" +
            "Overdue words: %d\n" +
            "Total reviews: %d",
            user.getUsername(),
            user.getTargetExam() != null ? user.getTargetExam() : "unknown",
            user.getTargetVocabSize() != null ? user.getTargetVocabSize() : 6000,
            user.getDailyNewWords() != null ? user.getDailyNewWords() : 20,
            user.getDifficultyLevel() != null ? user.getDifficultyLevel() : 3,
            user.getMemoryStrength() != null ? user.getMemoryStrength() : 1.0,
            user.getAvgRecallRate() != null ? user.getAvgRecallRate() : 0.0,
            user.getStreakDays() != null ? user.getStreakDays() : 0,
            totalLearned, masteredCount, reviewCount, learningCount,
            avgMastery, avgRetrievability,
            recentWindow, recentAccuracy * 100,
            overdueCount, reviews.size()
        );

        String systemPrompt =
            "You are a vocabulary learning coach. Analyze the user's learning data and provide " +
            "personalized recommendations in JSON format.\n\n" +
            "Return a JSON object with:\n" +
            "  \"recommendations\": an array of 3-5 specific, actionable recommendation strings\n" +
            "  \"analysis\": a brief summary paragraph analyzing the user's learning progress\n" +
            "  \"dailyGoal\": suggested daily new word count\n" +
            "  \"focusAreas\": array of areas the user should focus on\n\n" +
            "Respond in the same language as the user's target exam context. " +
            "Keep recommendations concise and actionable.";

        String userPrompt = "Please analyze my vocabulary learning progress and provide recommendations:\n\n"
                + statsSummary;

        // Attempt LLM-based recommendations
        if (llmConfig.isLlmConfigured()) {
            try {
                Map<String, Object> llmResult = callLlmJson(systemPrompt, userPrompt, 0.7);
                if (llmResult != null && !llmResult.isEmpty() && !llmResult.containsKey("raw")) {
                    llmResult.put("source", "llm");
                    llmResult.put("statsSummary", Map.of(
                        "totalLearned", totalLearned,
                        "masteredCount", masteredCount,
                        "overdueCount", overdueCount,
                        "recentAccuracy", Math.round(recentAccuracy * 100.0) / 100.0
                    ));
                    log.info("LLM recommendations generated for user={}", user.getUsername());
                    return llmResult;
                }
                log.warn("LLM returned unusable result, falling back for user={}", user.getUsername());
            } catch (Exception e) {
                log.warn("LLM recommendation failed for user={}: {}", user.getUsername(), e.getMessage());
            }
        }

        // Fallback: rule-based recommendations
        return buildFallbackRecommendations(user, totalLearned, masteredCount, overdueCount,
                recentAccuracy, avgMastery, avgRetrievability, learningCount, reviewCount);
    }

    /**
     * Build rule-based recommendations when LLM is unavailable.
     */
    private Map<String, Object> buildFallbackRecommendations(
            User user, long totalLearned, long masteredCount, long overdueCount,
            double recentAccuracy, double avgMastery, double avgRetrievability,
            long learningCount, long reviewCount) {

        List<String> recommendations = new ArrayList<>();

        // Overdue words
        if (overdueCount > 20) {
            recommendations.add("You have " + overdueCount + " overdue words. Prioritize reviewing these before learning new words.");
        } else if (overdueCount > 5) {
            recommendations.add("You have " + overdueCount + " overdue words. Try to clear them today.");
        }

        // Accuracy-based
        if (recentAccuracy > 0 && recentAccuracy < 0.5) {
            recommendations.add("Your recent accuracy is " + String.format("%.0f%%", recentAccuracy * 100)
                    + ". Consider reducing daily new words and focusing on review.");
        } else if (recentAccuracy >= 0.9) {
            recommendations.add("Excellent accuracy at " + String.format("%.0f%%", recentAccuracy * 100)
                    + "! You may be ready to increase daily new words or raise the difficulty level.");
        }

        // Mastery progress
        if (totalLearned > 0) {
            double masteryRate = (double) masteredCount / totalLearned;
            if (masteryRate < 0.2) {
                recommendations.add("Only " + String.format("%.0f%%", masteryRate * 100)
                        + " of your words are mastered. Focus on reinforcing words in the learning queue.");
            } else if (masteryRate > 0.6) {
                recommendations.add("Great mastery rate of " + String.format("%.0f%%", masteryRate * 100)
                        + ". Keep up the consistent review schedule.");
            }
        }

        // Retrievability
        if (avgRetrievability < 0.4) {
            recommendations.add("Average memory retrievability is low (" + String.format("%.2f", avgRetrievability)
                    + "). Consider shorter, more frequent study sessions.");
        }

        // Streak encouragement
        int streak = user.getStreakDays() != null ? user.getStreakDays() : 0;
        if (streak >= 7) {
            recommendations.add("Amazing " + streak + "-day streak! Consistency is the key to long-term retention.");
        } else if (streak == 0 && totalLearned > 0) {
            recommendations.add("Your streak has broken. Start a new streak today to build learning momentum.");
        }

        // Target progress
        int target = user.getTargetVocabSize() != null ? user.getTargetVocabSize() : 6000;
        if (totalLearned > 0 && totalLearned < target) {
            double progressPercent = (double) totalLearned / target * 100;
            int remaining = target - (int) totalLearned;
            recommendations.add("Progress toward " + target + " word target: "
                    + String.format("%.1f%%", progressPercent)
                    + " (" + remaining + " words remaining).");
        }

        // Default recommendation if none were generated
        if (recommendations.isEmpty()) {
            recommendations.add("Keep up your daily study routine. Consistent practice leads to mastery.");
        }

        String analysis = String.format(
            "You have learned %d words with %.0f%% mastery rate. " +
            "Recent accuracy is %.0f%% with %d words overdue for review.",
            totalLearned,
            totalLearned > 0 ? (double) masteredCount / totalLearned * 100 : 0.0,
            recentAccuracy * 100,
            overdueCount
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recommendations", recommendations);
        result.put("analysis", analysis);
        result.put("dailyGoal", user.getDailyNewWords() != null ? user.getDailyNewWords() : 20);
        result.put("focusAreas", buildFocusAreas(overdueCount, recentAccuracy, avgRetrievability));
        result.put("source", "fallback");
        result.put("statsSummary", Map.of(
            "totalLearned", totalLearned,
            "masteredCount", masteredCount,
            "overdueCount", overdueCount,
            "recentAccuracy", Math.round(recentAccuracy * 100.0) / 100.0
        ));

        log.info("Fallback recommendations generated for user={}: {} items",
                user.getUsername(), recommendations.size());
        return result;
    }

    /**
     * Determine focus areas based on learning metrics.
     */
    private List<String> buildFocusAreas(long overdueCount, double recentAccuracy,
                                          double avgRetrievability) {
        List<String> areas = new ArrayList<>();

        if (overdueCount > 10) {
            areas.add("Clear overdue review backlog");
        }
        if (recentAccuracy < 0.6) {
            areas.add("Improve recall accuracy through active practice");
        }
        if (avgRetrievability < 0.5) {
            areas.add("Strengthen memory retention with spaced review");
        }
        if (areas.isEmpty()) {
            areas.add("Maintain current study pace");
        }

        return areas;
    }

    // =========================================================================
    // Enhanced Scheduling Features
    // =========================================================================

    /**
     * Groups words into learning batches using intelligent grouping strategies.
     * Supports multiple grouping modes:
     * - DIFFICULTY: Group by difficulty level (1-5)
     * - TOPIC: Group by word topic/category from tags
     * - FREQUENCY: Group by frequency of appearance (high/medium/low)
     * - MASTERY: Group by mastery level (high/medium/low)
     * - PART_OF_SPEECH: Group by part of speech
     * - ERROR_RATE: Group by error rate in reviews
     * - MIXED: Intelligent mix of difficulty, topic, and mastery for balanced learning
     * 
     * @return grouped word lists with metadata about each group's composition
     */
    public Map<String, Object> groupWordsForLearning(User user, List<Map<String, Object>> words, 
            String groupMode, int groupSize) {
        log.info("Grouping {} words for user={} with mode={}, groupSize={}", 
                words.size(), user.getUsername(), groupMode, groupSize);

        Map<String, Object> result = new LinkedHashMap<>();
        
        if (words.isEmpty()) {
            result.put("groups", Collections.emptyList());
            result.put("totalGroups", 0);
            result.put("groupMode", groupMode);
            return result;
        }

        List<List<Map<String, Object>>> groups = new ArrayList<>();

        switch (groupMode.toUpperCase()) {
            case "DIFFICULTY":
                groups = groupByDifficulty(words, groupSize);
                break;
            case "TOPIC":
                groups = groupByTopic(words, groupSize);
                break;
            case "FREQUENCY":
                groups = groupByFrequency(words, groupSize);
                break;
            case "MASTERY":
                groups = groupByMastery(words, groupSize);
                break;
            case "PART_OF_SPEECH":
            case "POS":
                groups = groupByPartOfSpeech(words, groupSize);
                break;
            case "ERROR_RATE":
                groups = groupByErrorRate(words, groupSize);
                break;
            case "MIXED":
            default:
                groups = groupMixed(words, groupSize);
                break;
        }

        // Add group metadata
        List<Map<String, Object>> groupsWithMetadata = new ArrayList<>();
        int groupIndex = 1;
        for (List<Map<String, Object>> group : groups) {
            Map<String, Object> groupInfo = new LinkedHashMap<>();
            groupInfo.put("groupIndex", groupIndex++);
            groupInfo.put("wordCount", group.size());
            groupInfo.put("words", group);
            
            // Calculate group statistics
            groupInfo.putAll(calculateGroupStats(group));
            
            groupsWithMetadata.add(groupInfo);
        }

        result.put("groups", groupsWithMetadata);
        result.put("totalGroups", groups.size());
        result.put("totalWords", words.size());
        result.put("groupMode", groupMode);
        result.put("groupSize", groupSize);

        log.info("Created {} groups for user={}", groups.size(), user.getUsername());
        return result;
    }

    /**
     * Calculate statistics for a word group.
     */
    private Map<String, Object> calculateGroupStats(List<Map<String, Object>> group) {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        if (group.isEmpty()) {
            stats.put("avgDifficulty", 0.0);
            stats.put("avgMastery", 0.0);
            stats.put("difficultyRange", "");
            stats.put("masteryRange", "");
            return stats;
        }
        
        double sumDiff = 0;
        double sumMastery = 0;
        int minDiff = Integer.MAX_VALUE;
        int maxDiff = Integer.MIN_VALUE;
        double minMastery = Double.MAX_VALUE;
        double maxMastery = Double.MIN_VALUE;
        
        for (Map<String, Object> word : group) {
            Integer diff = (Integer) word.getOrDefault("difficulty", 3);
            Double mastery = (Double) word.getOrDefault("masteryLevel", 0.0);
            
            sumDiff += diff;
            sumMastery += mastery;
            minDiff = Math.min(minDiff, diff);
            maxDiff = Math.max(maxDiff, diff);
            minMastery = Math.min(minMastery, mastery);
            maxMastery = Math.max(maxMastery, mastery);
        }
        
        stats.put("avgDifficulty", Math.round(sumDiff / group.size() * 100.0) / 100.0);
        stats.put("avgMastery", Math.round(sumMastery / group.size() * 10000.0) / 10000.0);
        stats.put("difficultyRange", minDiff + "-" + maxDiff);
        stats.put("masteryRange", String.format("%.2f-%.2f", minMastery, maxMastery));
        
        // Determine difficulty label
        double avgDiff = sumDiff / group.size();
        if (avgDiff <= 2) stats.put("difficultyLabel", "简单");
        else if (avgDiff <= 3) stats.put("difficultyLabel", "中等");
        else if (avgDiff <= 4) stats.put("difficultyLabel", "较难");
        else stats.put("difficultyLabel", "困难");
        
        return stats;
    }

    private List<List<Map<String, Object>>> groupByDifficulty(List<Map<String, Object>> words, int groupSize) {
        Map<Integer, List<Map<String, Object>>> difficultyMap = new TreeMap<>();
        for (Map<String, Object> word : words) {
            Integer diff = (Integer) word.getOrDefault("difficulty", 3);
            difficultyMap.computeIfAbsent(diff, k -> new ArrayList<>()).add(word);
        }
        
        return createBalancedGroups(difficultyMap, groupSize);
    }

    private List<List<Map<String, Object>>> groupByTopic(List<Map<String, Object>> words, int groupSize) {
        Map<String, List<Map<String, Object>>> topicMap = new LinkedHashMap<>();
        for (Map<String, Object> word : words) {
            String tags = (String) word.getOrDefault("tags", "");
            String primaryTopic = tags.contains(",") ? tags.split(",")[0].trim() : tags;
            if (primaryTopic.isEmpty()) primaryTopic = "general";
            topicMap.computeIfAbsent(primaryTopic, k -> new ArrayList<>()).add(word);
        }
        
        return createBalancedGroups(topicMap, groupSize);
    }

    private List<List<Map<String, Object>>> groupByFrequency(List<Map<String, Object>> words, int groupSize) {
        Map<String, List<Map<String, Object>>> frequencyMap = new LinkedHashMap<>();
        for (Map<String, Object> word : words) {
            Integer rank = (Integer) word.getOrDefault("frequencyRank", 0);
            String freqBand;
            if (rank <= 1000) freqBand = "high";
            else if (rank <= 5000) freqBand = "medium";
            else freqBand = "low";
            frequencyMap.computeIfAbsent(freqBand, k -> new ArrayList<>()).add(word);
        }
        
        return createBalancedGroups(frequencyMap, groupSize);
    }

    private List<List<Map<String, Object>>> groupByMastery(List<Map<String, Object>> words, int groupSize) {
        Map<String, List<Map<String, Object>>> masteryMap = new LinkedHashMap<>();
        for (Map<String, Object> word : words) {
            Double mastery = (Double) word.getOrDefault("masteryLevel", 0.0);
            String masteryBand;
            if (mastery >= 0.8) masteryBand = "high";
            else if (mastery >= 0.5) masteryBand = "medium";
            else masteryBand = "low";
            masteryMap.computeIfAbsent(masteryBand, k -> new ArrayList<>()).add(word);
        }
        
        return createBalancedGroups(masteryMap, groupSize);
    }

    private List<List<Map<String, Object>>> groupByPartOfSpeech(List<Map<String, Object>> words, int groupSize) {
        Map<String, List<Map<String, Object>>> posMap = new LinkedHashMap<>();
        for (Map<String, Object> word : words) {
            String pos = (String) word.getOrDefault("partOfSpeech", "unknown");
            // Normalize POS
            if (pos != null) {
                pos = pos.toLowerCase();
                if (pos.contains("noun") || pos.contains("名词")) pos = "noun";
                else if (pos.contains("verb") || pos.contains("动词")) pos = "verb";
                else if (pos.contains("adj") || pos.contains("形容词")) pos = "adjective";
                else if (pos.contains("adv") || pos.contains("副词")) pos = "adverb";
                else if (pos.contains("prep") || pos.contains("介词")) pos = "preposition";
                else if (pos.contains("conj") || pos.contains("连词")) pos = "conjunction";
                else pos = "other";
            } else {
                pos = "unknown";
            }
            posMap.computeIfAbsent(pos, k -> new ArrayList<>()).add(word);
        }
        
        return createBalancedGroups(posMap, groupSize);
    }

    private List<List<Map<String, Object>>> groupByErrorRate(List<Map<String, Object>> words, int groupSize) {
        Map<String, List<Map<String, Object>>> errorMap = new LinkedHashMap<>();
        for (Map<String, Object> word : words) {
            Integer reviewCount = (Integer) word.getOrDefault("reviewCount", 0);
            Integer wrongCount = (Integer) word.getOrDefault("wrongCount", 0);
            double errorRate = reviewCount > 0 ? (double) wrongCount / reviewCount : 0.0;
            
            String errorBand;
            if (errorRate >= 0.5) errorBand = "high";
            else if (errorRate >= 0.2) errorBand = "medium";
            else errorBand = "low";
            errorMap.computeIfAbsent(errorBand, k -> new ArrayList<>()).add(word);
        }
        
        return createBalancedGroups(errorMap, groupSize);
    }

    /**
     * Advanced mixed grouping that creates balanced groups with varied difficulty, topics, and mastery levels.
     * Each group aims to have a mix of easy and challenging words to optimize learning.
     */
    private List<List<Map<String, Object>>> groupMixed(List<Map<String, Object>> words, int groupSize) {
        List<List<Map<String, Object>>> groups = new ArrayList<>();
        
        // Sort words by priority (combination of mastery and difficulty)
        List<Map<String, Object>> sortedWords = new ArrayList<>(words);
        sortedWords.sort((a, b) -> {
            Double masteryA = (Double) a.getOrDefault("masteryLevel", 0.0);
            Double masteryB = (Double) b.getOrDefault("masteryLevel", 0.0);
            Integer diffA = (Integer) a.getOrDefault("difficulty", 3);
            Integer diffB = (Integer) b.getOrDefault("difficulty", 3);
            
            // Lower mastery + higher difficulty = higher priority
            double priorityA = (1.0 - masteryA) * diffA;
            double priorityB = (1.0 - masteryB) * diffB;
            return Double.compare(priorityB, priorityA);
        });
        
        // Distribute words into groups with balance constraints
        int totalGroups = (int) Math.ceil((double) words.size() / groupSize);
        
        // Initialize groups
        for (int i = 0; i < totalGroups; i++) {
            groups.add(new ArrayList<>());
        }
        
        // Assign words using round-robin with balancing
        int groupIndex = 0;
        for (Map<String, Object> word : sortedWords) {
            // Find the best group to add this word to (most balanced)
            int bestGroup = findBestGroupForWord(groups, word, groupSize);
            groups.get(bestGroup).add(word);
            groupIndex = (groupIndex + 1) % totalGroups;
        }
        
        return groups;
    }

    /**
     * Find the best group for a word based on balance criteria.
     */
    private int findBestGroupForWord(List<List<Map<String, Object>>> groups, Map<String, Object> word, int groupSize) {
        int bestGroup = 0;
        double bestScore = Double.MAX_VALUE;
        
        Integer wordDiff = (Integer) word.getOrDefault("difficulty", 3);
        Double wordMastery = (Double) word.getOrDefault("masteryLevel", 0.0);
        
        for (int i = 0; i < groups.size(); i++) {
            List<Map<String, Object>> group = groups.get(i);
            
            // Skip if group is full
            if (group.size() >= groupSize) continue;
            
            // Calculate balance score for this group
            double score = calculateGroupBalanceScore(group, wordDiff, wordMastery);
            
            // Prefer groups with fewer words (to fill them up)
            score += group.size() * 0.1;
            
            if (score < bestScore) {
                bestScore = score;
                bestGroup = i;
            }
        }
        
        return bestGroup;
    }

    /**
     * Calculate how balanced adding a word would make a group.
     * Lower score = better balance.
     */
    private double calculateGroupBalanceScore(List<Map<String, Object>> group, int wordDiff, double wordMastery) {
        if (group.isEmpty()) {
            return 0;
        }
        
        // Calculate current group statistics
        double sumDiff = 0;
        double sumMastery = 0;
        for (Map<String, Object> w : group) {
            sumDiff += (Integer) w.getOrDefault("difficulty", 3);
            sumMastery += (Double) w.getOrDefault("masteryLevel", 0.0);
        }
        
        double avgDiff = sumDiff / group.size();
        double avgMastery = sumMastery / group.size();
        
        // Calculate new averages if we add this word
        double newAvgDiff = (sumDiff + wordDiff) / (group.size() + 1);
        double newAvgMastery = (sumMastery + wordMastery) / (group.size() + 1);
        
        // We want to keep difficulty around 3 (medium) and mastery around 0.5
        double diffBalance = Math.abs(newAvgDiff - 3.0);
        double masteryBalance = Math.abs(newAvgMastery - 0.5);
        
        // Combine scores (higher weight on difficulty balance)
        return diffBalance * 0.6 + masteryBalance * 0.4;
    }

    private <K> List<List<Map<String, Object>>> createBalancedGroups(Map<K, List<Map<String, Object>>> categoryMap, 
            int groupSize) {
        List<List<Map<String, Object>>> groups = new ArrayList<>();
        
        // Flatten while preserving category order for balanced distribution
        List<Map<String, Object>> allWords = new ArrayList<>();
        
        // Get all categories with their words
        List<List<Map<String, Object>>> categoryLists = new ArrayList<>(categoryMap.values());
        
        // Distribute words from different categories to create balanced groups
        int maxSize = categoryLists.stream().mapToInt(List::size).max().orElse(0);
        
        for (int i = 0; i < maxSize; i++) {
            for (List<Map<String, Object>> category : categoryLists) {
                if (i < category.size()) {
                    allWords.add(category.get(i));
                }
            }
        }
        
        // Now split into groups
        List<Map<String, Object>> currentGroup = new ArrayList<>();
        for (Map<String, Object> word : allWords) {
            currentGroup.add(word);
            if (currentGroup.size() >= groupSize) {
                groups.add(currentGroup);
                currentGroup = new ArrayList<>();
            }
        }
        
        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }
        
        return groups;
    }

    /**
     * Calculates optimal study time for the user based on historical learning patterns.
     * Analyzes:
     * - Hourly performance patterns (score, words studied, accuracy)
     * - Weekly patterns (day of week preferences)
     * - Session effectiveness metrics
     * 
     * Returns recommended times with detailed analysis and confidence scores.
     */
    public Map<String, Object> getOptimalStudyTimes(User user) {
        log.info("Calculating optimal study times for user={}", user.getUsername());
        Map<String, Object> result = new LinkedHashMap<>();

        List<LearningSession> sessions = learningSessionRepository.findByUserIdSince(user.getId(),
                LocalDateTime.now().minusDays(30));

        if (sessions.isEmpty()) {
            result.put("status", "insufficient_data");
            result.put("recommendedTimes", List.of("上午 9:00-11:00", "下午 2:00-4:00", "晚上 7:00-9:00"));
            result.put("message", "使用默认推荐学习时间");
            result.put("confidence", "低");
            return result;
        }

        Map<Integer, Double> hourlyPerformance = new HashMap<>();
        Map<Integer, Integer> hourlyCount = new HashMap<>();
        Map<Integer, Integer> hourlyWordsStudied = new HashMap<>();
        Map<Integer, Integer> hourlyCorrectCount = new HashMap<>();
        
        Map<String, Double> weekdayPerformance = new LinkedHashMap<>();
        Map<String, Integer> weekdayCount = new LinkedHashMap<>();
        String[] weekdays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        String[] weekdaysCn = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        for (int i = 0; i < weekdays.length; i++) {
            weekdayPerformance.put(weekdays[i], 0.0);
            weekdayCount.put(weekdays[i], 0);
        }

        for (LearningSession s : sessions) {
            if (s.getStartedAt() != null) {
                int hour = s.getStartedAt().getHour();
                String weekday = s.getStartedAt().getDayOfWeek().name();
                
                if (s.getSessionScore() != null) {
                    hourlyPerformance.merge(hour, s.getSessionScore(), Double::sum);
                    weekdayPerformance.merge(weekday, s.getSessionScore(), Double::sum);
                }
                hourlyCount.merge(hour, 1, Integer::sum);
                weekdayCount.merge(weekday, 1, Integer::sum);
                
                if (s.getWordsStudied() != null) {
                    hourlyWordsStudied.merge(hour, s.getWordsStudied(), Integer::sum);
                }
                if (s.getWordsCorrect() != null) {
                    hourlyCorrectCount.merge(hour, s.getWordsCorrect(), Integer::sum);
                }
            }
        }

        List<Map<String, Object>> hourlyAnalysis = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            final int hour = h;
            if (hourlyCount.containsKey(hour)) {
                Double totalScore = hourlyPerformance.get(hour);
                double avgScore = totalScore != null ? totalScore / hourlyCount.get(hour) : 0.0;
                Integer wordsStudied = hourlyWordsStudied.get(hour);
                double avgWords = wordsStudied != null ? wordsStudied / (double) hourlyCount.get(hour) : 0.0;
                double avgAccuracy = wordsStudied != null && wordsStudied > 0 
                        ? (hourlyCorrectCount.getOrDefault(hour, 0) / (double) wordsStudied) 
                        : 0;
                
                Map<String, Object> hourData = new LinkedHashMap<>();
                hourData.put("hour", hour);
                hourData.put("avgScore", Math.round(avgScore * 10000.0) / 10000.0);
                hourData.put("avgWordsStudied", Math.round(avgWords * 100.0) / 100.0);
                hourData.put("avgAccuracy", Math.round(avgAccuracy * 10000.0) / 10000.0);
                hourData.put("sessionCount", hourlyCount.get(hour));
                
                // Effectiveness score (combination of score and words studied)
                double effectiveness = avgScore * 0.6 + avgWords / 20.0 * 0.4;
                hourData.put("effectiveness", Math.round(effectiveness * 10000.0) / 10000.0);
                
                hourData.put("isOptimal", Arrays.stream(OPTIMAL_HOURS).anyMatch(oh -> oh == hour));
                
                hourlyAnalysis.add(hourData);
            }
        }

        hourlyAnalysis.sort((a, b) -> {
            Double ea = a.get("effectiveness") instanceof Double ? (Double) a.get("effectiveness") : 0.0;
            Double eb = b.get("effectiveness") instanceof Double ? (Double) b.get("effectiveness") : 0.0;
            return Double.compare(eb, ea);
        });

        List<Map<String, Object>> recommendedTimeSlots = new ArrayList<>();
        for (int i = 0; i < Math.min(3, hourlyAnalysis.size()); i++) {
            Map<String, Object> hourData = hourlyAnalysis.get(i);
            Integer hourObj = hourData.get("hour") instanceof Integer ? (Integer) hourData.get("hour") : 0;
            int hour = hourObj;
            
            String period;
            if (hour >= 6 && hour < 9) period = "早晨";
            else if (hour >= 9 && hour < 12) period = "上午";
            else if (hour >= 12 && hour < 14) period = "中午";
            else if (hour >= 14 && hour < 18) period = "下午";
            else if (hour >= 18 && hour < 22) period = "晚上";
            else period = "深夜";
            
            Map<String, Object> slot = new LinkedHashMap<>();
            slot.put("period", period);
            slot.put("timeRange", hour + ":00-" + (hour + 1) + ":00");
            slot.put("hour", hour);
            slot.put("avgScore", hourData.get("avgScore"));
            slot.put("avgWordsStudied", hourData.get("avgWordsStudied"));
            slot.put("avgAccuracy", hourData.get("avgAccuracy"));
            slot.put("effectiveness", hourData.get("effectiveness"));
            slot.put("reason", generateTimeSlotReason(hour, hourData));
            
            recommendedTimeSlots.add(slot);
        }

        // Weekly pattern analysis
        List<Map<String, Object>> weeklyAnalysis = new ArrayList<>();
        for (int i = 0; i < weekdays.length; i++) {
            String weekday = weekdays[i];
            if (weekdayCount.get(weekday) > 0) {
                double avgScore = weekdayPerformance.get(weekday) / weekdayCount.get(weekday);
                Map<String, Object> dayData = new LinkedHashMap<>();
                dayData.put("weekday", weekdaysCn[i]);
                dayData.put("weekdayEn", weekday);
                dayData.put("avgScore", Math.round(avgScore * 10000.0) / 10000.0);
                dayData.put("sessionCount", weekdayCount.get(weekday));
                weeklyAnalysis.add(dayData);
            }
        }
        weeklyAnalysis.sort((a, b) -> {
            Double sa = a.get("avgScore") instanceof Double ? (Double) a.get("avgScore") : 0.0;
            Double sb = b.get("avgScore") instanceof Double ? (Double) b.get("avgScore") : 0.0;
            return Double.compare(sb, sa);
        });

        // Determine confidence level
        String confidence = sessions.size() >= 20 ? "高" : sessions.size() >= 10 ? "中" : "低";

        result.put("status", "success");
        result.put("hourlyAnalysis", hourlyAnalysis);
        result.put("weeklyAnalysis", weeklyAnalysis);
        result.put("recommendedTimeSlots", recommendedTimeSlots);
        result.put("sampleSize", sessions.size());
        result.put("confidence", confidence);
        result.put("bestWeekday", weeklyAnalysis.isEmpty() ? "周一" : weeklyAnalysis.get(0).get("weekday"));

        log.info("Optimal study times for user {}: {} slots, confidence={}", 
                user.getUsername(), recommendedTimeSlots.size(), confidence);
        return result;
    }

    /**
     * Generate a reason for recommending a specific time slot.
     */
    private String generateTimeSlotReason(int hour, Map<String, Object> hourData) {
        double effectiveness = hourData.get("effectiveness") instanceof Double ? (Double) hourData.get("effectiveness") : 0.0;
        double avgScore = hourData.get("avgScore") instanceof Double ? (Double) hourData.get("avgScore") : 0.0;
        double avgWords = hourData.get("avgWordsStudied") instanceof Double ? (Double) hourData.get("avgWordsStudied") : 0.0;
        
        StringBuilder reason = new StringBuilder();
        
        if (effectiveness > 0.8) {
            reason.append("学习效率极高");
        } else if (effectiveness > 0.6) {
            reason.append("学习效率良好");
        } else {
            reason.append("学习效果不错");
        }
        
        if (avgScore > 0.8) {
            reason.append("，记忆准确率高");
        }
        
        if (avgWords > 15) {
            reason.append("，学习量较大");
        }
        
        return reason.toString();
    }

    /**
     * Adjusts user's difficulty level dynamically based on comprehensive learning performance analysis.
     * Factors considered:
     * - Review accuracy (primary factor)
     * - Memory stability trends
     * - Mastery level changes
     * - Learning speed
     * - Error rate patterns
     * 
     * Returns detailed adjustment recommendation with confidence score.
     */
    public Map<String, Object> adjustDifficultyLevel(User user) {
        log.info("Adjusting difficulty level for user={}", user.getUsername());
        Map<String, Object> result = new LinkedHashMap<>();

        List<ReviewRecord> recentReviews = reviewRecordRepository.findByUserIdOrderByReviewedAtDesc(user.getId())
                .stream().limit(50).collect(Collectors.toList());
        
        List<UserWord> userWords = userWordRepository.findByUserId(user.getId());

        if (recentReviews.size() < 10) {
            result.put("status", "insufficient_data");
            result.put("currentLevel", user.getDifficultyLevel());
            result.put("recommendation", "保持当前难度");
            result.put("reason", "需要至少10次复习记录来调整难度");
            return result;
        }

        int currentLevel = user.getDifficultyLevel() != null ? user.getDifficultyLevel() : 3;
        
        // Calculate accuracy
        long correctCount = recentReviews.stream().filter(r -> Boolean.TRUE.equals(r.getCorrect())).count();
        double accuracy = (double) correctCount / recentReviews.size();
        
        // Calculate recent vs earlier accuracy trend
        int midPoint = recentReviews.size() / 2;
        double recentAccuracy = midPoint > 0 
                ? recentReviews.subList(0, midPoint).stream().filter(r -> Boolean.TRUE.equals(r.getCorrect())).count() / (double) midPoint
                : accuracy;
        double earlierAccuracy = midPoint > 0 
                ? recentReviews.subList(midPoint, recentReviews.size()).stream().filter(r -> Boolean.TRUE.equals(r.getCorrect())).count() / (double) (recentReviews.size() - midPoint)
                : accuracy;
        double accuracyTrend = recentAccuracy - earlierAccuracy;

        // Calculate mastery statistics
        double avgMastery = userWords.isEmpty() ? 0.0 
                : userWords.stream().mapToDouble(uw -> uw.getMasteryLevel() != null ? uw.getMasteryLevel() : 0.0).average().orElse(0.0);
        
        // Calculate stability indicator (based on consecutive correct reviews)
        int currentStreak = 0;
        int maxStreak = 0;
        for (ReviewRecord r : recentReviews) {
            if (Boolean.TRUE.equals(r.getCorrect())) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 0;
            }
        }

        // Calculate error rate
        double errorRate = 1.0 - accuracy;

        // Combined score for difficulty adjustment
        double adjustmentScore = calculateAdjustmentScore(accuracy, accuracyTrend, avgMastery, maxStreak, currentLevel);
        
        String recommendation;
        String reason;
        int suggestedLevel = currentLevel;
        String confidence;

        if (adjustmentScore >= 80) {
            suggestedLevel = Math.min(5, currentLevel + 1);
            recommendation = suggestedLevel > currentLevel ? "提升难度" : "保持当前难度";
            reason = generateAdjustmentReason("increase", accuracy, accuracyTrend, avgMastery, maxStreak);
            confidence = "高";
        } else if (adjustmentScore >= 60) {
            suggestedLevel = currentLevel;
            recommendation = "保持当前难度";
            reason = generateAdjustmentReason("maintain", accuracy, accuracyTrend, avgMastery, maxStreak);
            confidence = "中";
        } else if (adjustmentScore >= 40) {
            suggestedLevel = currentLevel;
            recommendation = "保持当前难度并加强复习";
            reason = generateAdjustmentReason("maintain_with_review", accuracy, accuracyTrend, avgMastery, maxStreak);
            confidence = "中";
        } else {
            suggestedLevel = Math.max(1, currentLevel - 1);
            recommendation = suggestedLevel < currentLevel ? "降低难度" : "保持当前难度";
            reason = generateAdjustmentReason("decrease", accuracy, accuracyTrend, avgMastery, maxStreak);
            confidence = "高";
        }

        // Add detailed metrics
        result.put("status", "success");
        result.put("currentLevel", currentLevel);
        result.put("suggestedLevel", suggestedLevel);
        result.put("recommendation", recommendation);
        result.put("reason", reason);
        result.put("confidence", confidence);
        
        // Detailed metrics
        result.put("recentAccuracy", Math.round(accuracy * 10000.0) / 10000.0);
        result.put("recentAccuracyPct", String.format("%.1f%%", accuracy * 100));
        result.put("accuracyTrend", Math.round(accuracyTrend * 10000.0) / 10000.0);
        result.put("accuracyTrendLabel", accuracyTrend > 0.05 ? "上升" : accuracyTrend < -0.05 ? "下降" : "稳定");
        result.put("avgMasteryLevel", Math.round(avgMastery * 10000.0) / 10000.0);
        result.put("maxCorrectStreak", maxStreak);
        result.put("errorRate", Math.round(errorRate * 10000.0) / 10000.0);
        result.put("adjustmentScore", Math.round(adjustmentScore));
        result.put("sampleSize", recentReviews.size());

        // Next review recommendations based on adjustment
        if ("提升难度".equals(recommendation)) {
            result.put("nextReviewRecommendation", "建议下次复习时尝试更高难度的单词，挑战自己");
        } else if ("降低难度".equals(recommendation)) {
            result.put("nextReviewRecommendation", "建议复习更多基础单词，巩固基础后再提升难度");
        } else {
            result.put("nextReviewRecommendation", "继续保持当前学习节奏，定期复习已学单词");
        }

        log.info("Difficulty adjustment for user {}: current={}, suggested={}, accuracy={}%, score={}",
                user.getUsername(), currentLevel, suggestedLevel, String.format("%.0f", accuracy * 100), 
                Math.round(adjustmentScore));
        return result;
    }

    /**
     * Calculate a combined score for difficulty adjustment decisions.
     * Score ranges from 0-100, higher means more likely to increase difficulty.
     */
    private double calculateAdjustmentScore(double accuracy, double accuracyTrend, 
            double avgMastery, int maxStreak, int currentLevel) {
        
        double score = 0;
        
        // Accuracy (40% weight)
        score += accuracy * 40;
        
        // Accuracy trend (20% weight)
        score += Math.max(-10, Math.min(10, accuracyTrend * 100)) * 0.2;
        
        // Mastery level (20% weight)
        score += avgMastery * 20;
        
        // Streak bonus (10% weight)
        score += Math.min(10, maxStreak * 1.5);
        
        // Current level adjustment (10% weight) - lower levels get bonus to encourage progression
        score += (6 - currentLevel) * 1.5;
        
        return Math.max(0, Math.min(100, score));
    }

    /**
     * Generate a descriptive reason for the difficulty adjustment recommendation.
     */
    private String generateAdjustmentReason(String action, double accuracy, double accuracyTrend, 
            double avgMastery, int maxStreak) {
        StringBuilder reason = new StringBuilder();
        
        if ("increase".equals(action)) {
            reason.append("表现优秀，建议提升难度。");
            if (accuracy >= 0.9) reason.append(" 近期正确率极高");
            if (accuracyTrend > 0.05) reason.append("，学习效果持续提升");
            if (avgMastery >= 0.7) reason.append("，掌握程度良好");
            if (maxStreak >= 5) reason.append("，连续答对次数多");
        } else if ("decrease".equals(action)) {
            reason.append("建议降低难度以巩固基础。");
            if (accuracy < 0.5) reason.append(" 近期正确率偏低");
            if (accuracyTrend < -0.05) reason.append("，学习效果有所下降");
            if (avgMastery < 0.4) reason.append("，掌握程度需要加强");
        } else if ("maintain_with_review".equals(action)) {
            reason.append("保持当前难度，建议增加复习频率。");
            if (accuracy < 0.7) reason.append(" 近期正确率一般");
            if (accuracyTrend < 0) reason.append("，需要加强复习巩固");
        } else {
            reason.append("保持当前难度。");
            if (accuracy >= 0.7) reason.append(" 近期表现稳定");
            if (accuracyTrend >= 0) reason.append("，学习效果平稳");
        }
        
        return reason.toString();
    }

    /**
     * Generates a personalized daily learning plan based on comprehensive analysis:
     * - User's memory diagnosis and learning efficiency
     * - Optimal study times from historical patterns
     * - Current learning progress and word status distribution
     * - Urgent words requiring immediate attention
     * - Available time slots and user preferences
     * 
     * Returns detailed plan with prioritized tasks, timing suggestions, and goals.
     */
    public Map<String, Object> generateDailyPlan(User user, int availableMinutes) {
        log.info("Generating daily plan for user={}, availableMinutes={}", user.getUsername(), availableMinutes);
        Map<String, Object> plan = new LinkedHashMap<>();

        List<UserWord> userWords = userWordRepository.findByUserId(user.getId());
        LocalDateTime now = LocalDateTime.now();

        // Calculate word statistics
        long overdueCount = userWords.stream()
                .filter(uw -> uw.getNextReview() != null && uw.getNextReview().isBefore(now))
                .filter(uw -> !"mastered".equals(uw.getStatus()))
                .count();

        long learningCount = userWords.stream()
                .filter(uw -> "learning".equals(uw.getStatus()))
                .count();

        long reviewCount = userWords.stream()
                .filter(uw -> "review".equals(uw.getStatus()))
                .count();

        long masteredCount = userWords.stream()
                .filter(uw -> "mastered".equals(uw.getStatus()))
                .count();

        // Calculate urgent words count
        int urgentWordsCount = (int) userWords.stream()
                .filter(uw -> !"mastered".equals(uw.getStatus()))
                .filter(uw -> {
                    double score = calculateUrgencyScore(uw, now);
                    return score > 10;
                })
                .count();

        // Get user preferences
        int dailyNewWords = user.getDailyNewWords() != null ? user.getDailyNewWords() : 20;
        
        // Calculate average time per word based on user's learning speed
        double avgMinutesPerWord = calculateAvgMinutesPerWord(user);

        // Allocate time based on priority and availability
        int reviewTime, newWordTime, deepLearningTime, weakReviewTime;
        
        // Priority: overdue > urgent > new words > deep learning > weak words review
        int maxReviewTime = (int) (availableMinutes * 0.5);
        int maxNewWordTime = (int) (availableMinutes * 0.3);
        int maxDeepLearningTime = (int) (availableMinutes * 0.15);
        int maxWeakReviewTime = (int) (availableMinutes * 0.15);

        // Overdue review time
        reviewTime = (int) Math.min(overdueCount * avgMinutesPerWord, maxReviewTime);
        
        // Remaining time after overdue review
        int remainingTime = availableMinutes - reviewTime;
        
        // New words time (limited by remaining time)
        newWordTime = (int) Math.min(dailyNewWords * avgMinutesPerWord, 
                Math.min(maxNewWordTime, remainingTime * 0.6));
        
        remainingTime -= newWordTime;
        
        // Deep learning time (for words with low mastery)
        deepLearningTime = (int) Math.min(maxDeepLearningTime, remainingTime * 0.5);
        
        // Weak words review time (for words with high error rate)
        weakReviewTime = Math.max(0, remainingTime - deepLearningTime);

        // Generate detailed tasks
        List<Map<String, Object>> tasks = new ArrayList<>();

        if (reviewTime > 0) {
            Map<String, Object> reviewTask = new LinkedHashMap<>();
            reviewTask.put("type", "review");
            reviewTask.put("taskName", "复习已过期单词");
            reviewTask.put("durationMinutes", reviewTime);
            reviewTask.put("targetCount", (int) (reviewTime / avgMinutesPerWord));
            reviewTask.put("priority", "high");
            reviewTask.put("description", "优先复习已过期的单词，防止遗忘");
            reviewTask.put("wordCount", (int) overdueCount);
            reviewTask.put("suggestedMethod", "使用卡片复习模式，专注于回忆单词含义");
            tasks.add(reviewTask);
        }

        if (urgentWordsCount > 0 && reviewTime > 0) {
            Map<String, Object> urgentTask = new LinkedHashMap<>();
            urgentTask.put("type", "urgent_review");
            urgentTask.put("taskName", "紧急单词强化");
            urgentTask.put("durationMinutes", Math.min(15, reviewTime));
            urgentTask.put("targetCount", Math.min(5, urgentWordsCount));
            urgentTask.put("priority", "high");
            urgentTask.put("description", "复习即将遗忘的高风险单词");
            urgentTask.put("wordCount", urgentWordsCount);
            urgentTask.put("suggestedMethod", "重点关注掌握度低且错误率高的单词");
            tasks.add(urgentTask);
        }

        if (newWordTime > 0) {
            Map<String, Object> newWordTask = new LinkedHashMap<>();
            newWordTask.put("type", "new_words");
            newWordTask.put("taskName", "学习新单词");
            newWordTask.put("durationMinutes", newWordTime);
            newWordTask.put("targetCount", (int) (newWordTime / avgMinutesPerWord));
            newWordTask.put("priority", "medium");
            newWordTask.put("description", "学习新单词，建立初步记忆");
            newWordTask.put("wordCount", dailyNewWords);
            newWordTask.put("suggestedMethod", "结合例句和图片，多感官记忆");
            tasks.add(newWordTask);
        }

        if (weakReviewTime > 0) {
            Map<String, Object> weakTask = new LinkedHashMap<>();
            weakTask.put("type", "weak_review");
            weakTask.put("taskName", "薄弱单词巩固");
            weakTask.put("durationMinutes", weakReviewTime);
            weakTask.put("targetCount", (int) (weakReviewTime / avgMinutesPerWord));
            weakTask.put("priority", "low");
            weakTask.put("description", "复习错误率较高的单词，巩固记忆");
            weakTask.put("suggestedMethod", "回顾错题，分析错误原因，加强练习");
            tasks.add(weakTask);
        }

        // If no tasks generated, create a default plan based on user's learning status
        if (tasks.isEmpty()) {
            if (masteredCount > 0 && masteredCount < userWords.size()) {
                Map<String, Object> reviewTask = new LinkedHashMap<>();
                reviewTask.put("type", "review");
                reviewTask.put("taskName", "复习已掌握单词");
                reviewTask.put("durationMinutes", 15);
                reviewTask.put("targetCount", Math.min(5, (int) masteredCount));
                reviewTask.put("priority", "medium");
                reviewTask.put("description", "巩固已掌握的单词，防止遗忘");
                reviewTask.put("wordCount", (int) masteredCount);
                tasks.add(reviewTask);
            }
            
            if (learningCount > 0 || userWords.isEmpty()) {
                Map<String, Object> newTask = new LinkedHashMap<>();
                newTask.put("type", "new_words");
                newTask.put("taskName", "学习新单词");
                newTask.put("durationMinutes", (int) (dailyNewWords * avgMinutesPerWord));
                newTask.put("targetCount", dailyNewWords);
                newTask.put("priority", "medium");
                newTask.put("description", "学习新单词，建立初步记忆");
                newTask.put("wordCount", dailyNewWords);
                tasks.add(newTask);
            }
        }

        // Get optimal study times
        Map<String, Object> optimalTimes = getOptimalStudyTimes(user);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recommendedTimeSlots = (List<Map<String, Object>>) optimalTimes.get("recommendedTimeSlots");
        
        // Generate schedule based on optimal times
        List<Map<String, Object>> schedule = generateSchedule(tasks, recommendedTimeSlots);

        // Calculate daily goals
        int totalTargetWords = tasks.stream()
                .mapToInt(t -> t.get("targetCount") instanceof Integer ? (Integer) t.get("targetCount") : 0)
                .sum();
        
        // Calculate progress summary
        int totalWords = userWords.size();
        int learnedWords = (int) (learningCount + reviewCount + masteredCount);
        
        // Build plan result
        plan.put("status", "success");
        plan.put("planDate", now.toLocalDate().toString());
        plan.put("availableMinutes", availableMinutes);
        
        // Statistics
        plan.put("wordStatistics", Map.of(
                "total", totalWords,
                "learning", learningCount,
                "review", reviewCount,
                "mastered", masteredCount,
                "overdue", overdueCount,
                "urgent", urgentWordsCount
        ));
        
        // Tasks
        plan.put("tasks", tasks);
        plan.put("schedule", schedule);
        
        // Goals
        plan.put("dailyGoals", Map.of(
                "targetWords", totalTargetWords,
                "newWords", newWordTime > 0 ? (int) (newWordTime / avgMinutesPerWord) : 0,
                "reviewWords", reviewTime > 0 ? (int) (reviewTime / avgMinutesPerWord) : 0,
                "deepLearningWords", deepLearningTime > 0 ? (int) (deepLearningTime / (avgMinutesPerWord * 2)) : 0
        ));
        
        // Time recommendations
        plan.put("optimalStudyTimes", optimalTimes);
        
        // Suggestions
        List<String> suggestions = generatePlanSuggestions(user, overdueCount, urgentWordsCount, totalTargetWords);
        plan.put("suggestions", suggestions);

        log.info("Daily plan generated for user {}: {} tasks, {} target words, {} minutes",
                user.getUsername(), tasks.size(), totalTargetWords, availableMinutes);
        return plan;
    }

    /**
     * Calculate average minutes per word based on user's learning history.
     */
    private double calculateAvgMinutesPerWord(User user) {
        List<LearningSession> recentSessions = learningSessionRepository.findByUserIdSince(user.getId(),
                LocalDateTime.now().minusDays(14));
        
        if (recentSessions.isEmpty()) {
            return 1.5;
        }
        
        int totalMinutes = 0;
        int totalWords = 0;
        
        for (LearningSession session : recentSessions) {
            if (session.getWordsStudied() != null && session.getWordsStudied() > 0) {
                totalWords += session.getWordsStudied();
                
                // Calculate duration from start and end times
                if (session.getStartedAt() != null && session.getEndedAt() != null) {
                    totalMinutes += (int) ChronoUnit.MINUTES.between(session.getStartedAt(), session.getEndedAt());
                } else if (session.getStartedAt() != null) {
                    // Estimate if endedAt is null (session still active)
                    totalMinutes += (int) ChronoUnit.MINUTES.between(session.getStartedAt(), LocalDateTime.now());
                }
            }
        }
        
        return totalWords > 0 ? (double) totalMinutes / totalWords : 1.5;
    }

    /**
     * Generate a time schedule for tasks based on optimal study times.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> generateSchedule(List<Map<String, Object>> tasks, 
            List<Map<String, Object>> timeSlots) {
        List<Map<String, Object>> schedule = new ArrayList<>();
        
        if (timeSlots == null || timeSlots.isEmpty()) {
            // Default schedule if no optimal times
            int currentMinute = 0;
            for (Map<String, Object> task : tasks) {
                Integer durationObj = task.get("durationMinutes") instanceof Integer ? (Integer) task.get("durationMinutes") : 0;
            int duration = durationObj;
                Map<String, Object> slot = new LinkedHashMap<>();
                slot.put("taskType", task.get("type"));
                slot.put("taskName", task.get("taskName"));
                slot.put("startMinute", currentMinute);
                slot.put("durationMinutes", duration);
                slot.put("endMinute", currentMinute + duration);
                schedule.add(slot);
                currentMinute += duration;
            }
            return schedule;
        }
        
        // Assign tasks to optimal time slots
        int taskIndex = 0;
        for (Map<String, Object> timeSlot : timeSlots) {
            if (taskIndex >= tasks.size()) break;
            
            Integer hourObj = timeSlot.get("hour") instanceof Integer ? (Integer) timeSlot.get("hour") : 0;
            int hour = hourObj;
            int startMinute = hour * 60;
            
            while (taskIndex < tasks.size()) {
                Map<String, Object> task = tasks.get(taskIndex);
                Integer durationObj = task.get("durationMinutes") instanceof Integer ? (Integer) task.get("durationMinutes") : 0;
                int duration = durationObj;
                
                String priority = task.get("priority") instanceof String ? (String) task.get("priority") : "medium";
                if (!"high".equals(priority) && !"medium".equals(priority)) {
                    break;
                }
                
                Map<String, Object> slot = new LinkedHashMap<>();
                slot.put("taskType", task.get("type"));
                slot.put("taskName", task.get("taskName"));
                slot.put("startMinute", startMinute);
                slot.put("durationMinutes", duration);
                slot.put("endMinute", startMinute + duration);
                slot.put("timeSlot", timeSlot.get("period") + " " + timeSlot.get("timeRange"));
                slot.put("reason", timeSlot.get("reason"));
                schedule.add(slot);
                
                startMinute += duration;
                taskIndex++;
                
                // Don't assign more than 90 minutes to one time slot
                if (startMinute - hour * 60 >= 90) {
                    break;
                }
            }
        }
        
        // Assign remaining tasks
        int lastEndMinute = schedule.isEmpty() ? 0 : 
                (schedule.get(schedule.size() - 1).get("endMinute") instanceof Integer 
                    ? (Integer) schedule.get(schedule.size() - 1).get("endMinute") 
                    : 0);
        while (taskIndex < tasks.size()) {
            Map<String, Object> task = tasks.get(taskIndex);
            Integer durationObj = task.get("durationMinutes") instanceof Integer ? (Integer) task.get("durationMinutes") : 0;
            int duration = durationObj;
            
            Map<String, Object> slot = new LinkedHashMap<>();
            slot.put("taskType", task.get("type"));
            slot.put("taskName", task.get("taskName"));
            slot.put("startMinute", lastEndMinute);
            slot.put("durationMinutes", duration);
            slot.put("endMinute", lastEndMinute + duration);
            schedule.add(slot);
            
            lastEndMinute += duration;
            taskIndex++;
        }
        
        return schedule;
    }

    /**
     * Generate suggestions for the daily plan.
     */
    private List<String> generatePlanSuggestions(User user, long overdueCount, 
            int urgentWordsCount, int totalTargetWords) {
        List<String> suggestions = new ArrayList<>();
        
        if (overdueCount > 20) {
            suggestions.add("⚠️ 过期单词较多，建议优先完成复习任务");
        }
        
        if (urgentWordsCount > 10) {
            suggestions.add("🔴 有较多紧急单词需要复习，注意安排时间");
        }
        
        if (totalTargetWords < 10) {
            suggestions.add("💡 今日学习量较少，可以适当增加新单词学习");
        } else if (totalTargetWords > 50) {
            suggestions.add("⏰ 今日学习任务较重，注意劳逸结合");
        }
        
        suggestions.add("📚 学习新单词时，建议结合例句和语境理解");
        suggestions.add("🔄 定期复习是保持记忆的关键，不要跳过复习任务");
        
        return suggestions;
    }

    /**
     * Identifies words that need immediate attention and prioritizes them for review.
     * Uses a comprehensive urgency scoring algorithm considering:
     * - Overdue status and time elapsed since last review
     * - Mastery level and forgetting curve predictions
     * - Error rate and review history
     * - Word difficulty and importance
     * - Memory stability based on consecutive reviews
     * 
     * Returns detailed urgent word list with risk levels and recommended actions.
     */
    public Map<String, Object> getUrgentWords(User user, int limit) {
        log.info("Getting urgent words for user={}, limit={}", user.getUsername(), limit);
        Map<String, Object> result = new LinkedHashMap<>();

        List<UserWord> userWords = userWordRepository.findByUserId(user.getId());
        LocalDateTime now = LocalDateTime.now();

        List<Map<String, Object>> urgentList = new ArrayList<>();
        int criticalCount = 0;
        int highCount = 0;
        int mediumCount = 0;

        for (UserWord uw : userWords) {
            if ("mastered".equals(uw.getStatus())) continue;

            Map<String, Object> urgencyData = calculateUrgencyData(uw, now);
            urgentList.add(urgencyData);
            
            // Count by level
            String level = (String) urgencyData.get("urgencyLevel");
            if ("critical".equals(level)) criticalCount++;
            else if ("high".equals(level)) highCount++;
            else if ("medium".equals(level)) mediumCount++;
        }

        // Sort by urgency score descending
        urgentList.sort((a, b) -> {
            Double ua = a.get("urgencyScore") instanceof Double ? (Double) a.get("urgencyScore") : 0.0;
            Double ub = b.get("urgencyScore") instanceof Double ? (Double) b.get("urgencyScore") : 0.0;
            return Double.compare(ub, ua);
        });

        // Limit results
        List<Map<String, Object>> limitedList = urgentList.subList(0, Math.min(limit, urgentList.size()));

        // Build result
        result.put("status", "success");
        result.put("urgentWords", limitedList);
        result.put("totalUrgent", urgentList.size());
        result.put("criticalCount", criticalCount);
        result.put("highCount", highCount);
        result.put("mediumCount", mediumCount);
        
        // Summary
        String summary;
        if (criticalCount > 5) {
            summary = String.format("有%d个单词即将遗忘，建议立即复习！", criticalCount);
        } else if (criticalCount > 0) {
            summary = String.format("有%d个单词需要紧急复习，%d个单词建议尽快复习。", criticalCount, highCount);
        } else if (highCount > 0) {
            summary = String.format("有%d个单词建议尽快复习。", highCount);
        } else {
            summary = "当前没有紧急需要复习的单词，继续保持！";
        }
        result.put("summary", summary);

        log.info("Returned {} urgent words for user={} (critical={}, high={}, medium={})", 
                limitedList.size(), user.getUsername(), criticalCount, highCount, mediumCount);
        return result;
    }

    /**
     * Calculate comprehensive urgency data for a user word.
     */
    private Map<String, Object> calculateUrgencyData(UserWord uw, LocalDateTime now) {
        Map<String, Object> data = new LinkedHashMap<>();
        
        double urgencyScore = calculateUrgencyScore(uw, now);
        String urgencyLevel = getUrgencyLevel(urgencyScore);
        String riskReason = generateRiskReason(uw, now, urgencyScore);
        
        // Basic info
        data.put("userWordId", uw.getId());
        data.put("wordId", uw.getWordId());
        data.put("masteryLevel", uw.getMasteryLevel());
        data.put("intervalDays", uw.getIntervalDays());
        data.put("nextReview", uw.getNextReview() != null ? uw.getNextReview().toString() : null);
        data.put("status", uw.getStatus());
        data.put("reviewCount", uw.getReviewCount());
        data.put("wrongCount", uw.getWrongCount());
        
        // Urgency info
        data.put("urgencyScore", Math.round(urgencyScore * 100.0) / 100.0);
        data.put("urgencyLevel", urgencyLevel);
        data.put("riskReason", riskReason);
        data.put("urgent", urgencyScore > 10);
        
        // Word info
        if (uw.getWord() != null) {
            Word w = uw.getWord();
            data.put("word", w.getWord());
            data.put("definitionCn", w.getDefinitionCn());
            data.put("difficulty", w.getDifficulty());
            data.put("tags", w.getTags());
        }
        
        // Forgetting prediction
        data.put("forgettingRisk", calculateForgettingRisk(uw, now));
        
        return data;
    }

    /**
     * Calculate urgency score with comprehensive factors.
     */
    private double calculateUrgencyScore(UserWord uw, LocalDateTime now) {
        double score = 0.0;

        // Factor 1: Overdue penalty (40% weight)
        if (uw.getNextReview() != null) {
            long hoursOverdue = ChronoUnit.HOURS.between(uw.getNextReview(), now);
            if (hoursOverdue > 0) {
                // Exponential penalty for overdue time
                score += Math.min(40, hoursOverdue * 0.8);
            } else {
                // Bonus for early review
                long hoursUntilReview = ChronoUnit.HOURS.between(now, uw.getNextReview());
                if (hoursUntilReview < 24) {
                    score += (24 - hoursUntilReview) * 0.3;
                }
            }
        }

        // Factor 2: Mastery deficit (30% weight)
        double mastery = uw.getMasteryLevel() != null ? uw.getMasteryLevel() : 0.0;
        score += (1.0 - mastery) * 30.0;

        // Factor 3: Error rate (20% weight)
        int reviewCount = uw.getReviewCount() != null ? uw.getReviewCount() : 0;
        int wrongCount = uw.getWrongCount() != null ? uw.getWrongCount() : 0;
        double errorRate = reviewCount > 0 ? (double) wrongCount / reviewCount : 0.0;
        score += errorRate * 20.0;

        // Factor 4: Memory stability (10% weight)
        // Stability based on consecutive correct reviews
        int stabilityBonus = Math.min(10, reviewCount - wrongCount);
        score -= stabilityBonus * 0.5;

        // Factor 5: Difficulty multiplier
        double difficulty = 3.0;
        try {
            if (uw.getWord() != null && uw.getWord().getDifficulty() != null) {
                difficulty = uw.getWord().getDifficulty();
            }
        } catch (Exception e) {
            // Lazy loading failure
        }
        score *= (1.0 + (difficulty - 3.0) * 0.1);

        // Factor 6: Time since last review
        if (uw.getLastReviewed() != null) {
            long daysSinceLastReview = ChronoUnit.DAYS.between(uw.getLastReviewed(), now);
            if (daysSinceLastReview > 7) {
                score += (daysSinceLastReview - 7) * 2;
            }
        }

        return Math.max(0, score);
    }

    /**
     * Determine urgency level based on score.
     */
    private String getUrgencyLevel(double score) {
        if (score >= 30) return "critical";
        if (score >= 20) return "high";
        if (score >= 10) return "medium";
        return "low";
    }

    /**
     * Generate a descriptive reason for the urgency.
     */
    private String generateRiskReason(UserWord uw, LocalDateTime now, double score) {
        StringBuilder reason = new StringBuilder();
        
        double mastery = uw.getMasteryLevel() != null ? uw.getMasteryLevel() : 0.0;
        int reviewCount = uw.getReviewCount() != null ? uw.getReviewCount() : 0;
        int wrongCount = uw.getWrongCount() != null ? uw.getWrongCount() : 0;
        
        if (uw.getNextReview() != null) {
            long hoursOverdue = ChronoUnit.HOURS.between(uw.getNextReview(), now);
            if (hoursOverdue > 24) {
                reason.append("已过期").append(hoursOverdue / 24).append("天，");
            } else if (hoursOverdue > 0) {
                reason.append("已过期").append(hoursOverdue).append("小时，");
            }
        }
        
        if (mastery < 0.3) {
            reason.append("掌握度很低，");
        } else if (mastery < 0.5) {
            reason.append("掌握度较低，");
        }
        
        double errorRate = reviewCount > 0 ? (double) wrongCount / reviewCount : 0.0;
        if (errorRate > 0.5) {
            reason.append("错误率高，");
        } else if (errorRate > 0.3) {
            reason.append("错误率较高，");
        }
        
        if (reviewCount > 0 && reviewCount == wrongCount) {
            reason.append("从未答对过，");
        }
        
        if (reason.length() > 0) {
            reason.setLength(reason.length() - 1);
            reason.append("，遗忘风险高");
        } else {
            reason.append("遗忘风险较高");
        }
        
        return reason.toString();
    }

    /**
     * Calculate forgetting risk percentage based on Ebbinghaus-like decay.
     */
    private double calculateForgettingRisk(UserWord uw, LocalDateTime now) {
        double mastery = uw.getMasteryLevel() != null ? uw.getMasteryLevel() : 0.0;
        
        if (mastery >= 0.9) return 5; // Very low risk
        if (mastery >= 0.7) return 15; // Low risk
        
        // Calculate time since last review
        long daysSinceReview = 1;
        if (uw.getLastReviewed() != null) {
            daysSinceReview = ChronoUnit.DAYS.between(uw.getLastReviewed(), now);
        }
        
        // Ebbinghaus-like decay: R = e^(-t/S) * M
        // Where S is stability (based on mastery and interval), t is time
        double stability = (mastery * 2 + 1) * (uw.getIntervalDays() != null ? uw.getIntervalDays() : 1);
        double decay = Math.exp(-daysSinceReview / stability);
        
        // Risk = (1 - remaining memory) * 100
        double remainingMemory = decay * mastery + (1 - decay) * 0.1;
        double risk = (1 - remainingMemory) * 100;
        
        return Math.round(Math.min(99, Math.max(5, risk)));
    }

    // =========================================================================
    // Agent Identity
    // =========================================================================

    @Override
    public String getName() {
        return "scheduling";
    }
}
