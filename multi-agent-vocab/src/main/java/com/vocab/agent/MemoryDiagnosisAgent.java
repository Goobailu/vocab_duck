package com.vocab.agent;

import com.vocab.dto.DiagnosisReport;
import com.vocab.dto.ForgettingCurvePoint;
import com.vocab.entity.LearningSession;
import com.vocab.entity.ReviewRecord;
import com.vocab.entity.User;
import com.vocab.entity.UserWord;
import com.vocab.repository.LearningSessionRepository;
import com.vocab.repository.ReviewRecordRepository;
import com.vocab.repository.UserRepository;
import com.vocab.repository.UserWordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Memory Diagnosis Agent - analyzes a user's memory patterns by fitting
 * personalized forgetting curves, assessing mastery distribution, estimating
 * optimal learning capacity, and identifying weak/strong areas.
 * 
 * Enhanced features include:
 * - Advanced forgetting curve fitting with multiple model comparison
 * - Memory pattern recognition (visual, auditory, semantic learners)
 * - Learning efficiency analysis (time distribution, optimal study periods)
 * - Predictive analytics for future forgetting
 * - Session-based learning pattern analysis
 */
@Component
public class MemoryDiagnosisAgent extends BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(MemoryDiagnosisAgent.class);

    @Autowired
    private UserWordRepository userWordRepository;

    @Autowired
    private ReviewRecordRepository reviewRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LearningSessionRepository learningSessionRepository;

    /** Interval bin boundaries (in days) for forgetting curve fitting. */
    private static final int[] INTERVAL_BINS = {1, 2, 3, 5, 7, 14, 21, 30};

    /** Days at which to generate curve visualization points. */
    private static final int[] CURVE_DAYS = {0, 1, 2, 3, 5, 7, 10, 14, 21, 30};

    /** Default stability parameter when insufficient data is available. */
    private static final double DEFAULT_STABILITY = 3.0;

    /** Mastery band boundaries (percentages). */
    private static final double[] MASTERY_BANDS = {0.0, 0.2, 0.4, 0.6, 0.8, 1.0};

    private static final String[] MASTERY_BAND_LABELS = {
        "0-20%", "20-40%", "40-60%", "60-80%", "80-100%"
    };

    // =========================================================================
    // Memory Formula Constants (Spaced Repetition System Core Parameters)
    // =========================================================================
    
    /** Base decay constant λ (from S=1.07, d=0.094: λ = S × d ≈ 0.1) */
    private static final double BASE_DECAY_CONSTANT_LAMBDA = 0.1;
    
    /** Learning quality weight α (affects strength growth based on recall quality) */
    private static final double QUALITY_WEIGHT_ALPHA = 0.5;
    
    /** Difficulty weight β (affects strength reduction based on word difficulty) */
    private static final double DIFFICULTY_WEIGHT_BETA = 0.15;
    
    /** Baseline memory strength at initial learning */
    private static final double INITIAL_STRENGTH = 1.0;
    
    /** Maximum cognitive load C_max (based on typical learning capacity) */
    private static final int MAX_COGNITIVE_LOAD = 122;
    
    /** Review word cognitive weight W_r (cost per review word) */
    private static final double REVIEW_COGNITIVE_WEIGHT = 1.0;
    
    /** New word cognitive weight W_n (cost per new word, typically higher) */
    private static final double NEW_WORD_COGNITIVE_WEIGHT = 2.0;

    

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Run a full memory diagnosis for the given user.
     *
     * @param user the user to diagnose
     * @return a comprehensive DiagnosisReport
     */
    public DiagnosisReport diagnose(User user) {
        log.info("Starting memory diagnosis for user: {}", user.getUsername());

        List<UserWord> userWords = userWordRepository.findByUserId(user.getId());
        List<ReviewRecord> reviews = reviewRecordRepository.findByUserIdOrderByReviewedAtAsc(user.getId());

        // Return empty diagnosis when there is no learning data
        if (userWords.isEmpty() && reviews.isEmpty()) {
            log.info("No learning data for user {}, returning default diagnosis", user.getUsername());
            return buildEmptyDiagnosis(user);
        }

        // Step 1: Calculate memory strength using the SRS formula
        // Formula: S_n = S_{n-1} × e^[α(Q-3)] × (1 - β × D)
        double memoryStrength = calculateMemoryStrength(user, userWords, reviews);
        
        // Step 2: Calculate forgetting decay rate
        // Formula: d = λ / S
        double forgettingRate = BASE_DECAY_CONSTANT_LAMBDA / memoryStrength;
        
        // Step 3: Calculate memory half-life
        // Formula: T₁/₂ = ln(2) / d
        double halfLife = Math.log(2) / forgettingRate;

        // Step 2: Analyze mastery distribution
        Map<String, Object> masteryDistribution = analyzeMastery(userWords);

        // Step 3: Estimate optimal daily capacity
        int optimalDaily = estimateCapacity(user, reviews);

        // Step 4: Identify weak and strong areas
        Map<String, List<String>> areas = identifyAreas(userWords);
        List<String> weakAreas = areas.get("weak");
        List<String> strongAreas = areas.get("strong");

        // Step 5: Build curve points for visualization
        List<ForgettingCurvePoint> curvePoints = buildCurvePoints(memoryStrength, reviews);

        // Step 6: Analyze learning patterns (enhanced feature)
        Map<String, Object> learningPatterns = analyzeLearningPatterns(user);

        // Step 7: Identify memory style (enhanced feature)
        Map<String, Object> memoryStyle = identifyMemoryStyle(user, userWords, reviews);

        // Step 8: Generate predictive analysis (enhanced feature)
        Map<String, Object> predictiveAnalysis = generatePredictiveAnalysis(user, memoryStrength, userWords);

        // Step 10: Generate LLM-powered insights (with fallback)
        String insights = generateInsights(user, memoryStrength, masteryDistribution,
                optimalDaily, weakAreas, strongAreas, reviews);

        // Step 11: Build recommendations (enhanced with new insights)
        List<String> recommendations = buildRecommendations(memoryStrength, optimalDaily,
                weakAreas, userWords, reviews);

        // Step 12: Update user's memory strength and persist
        double normalizedStrength = Math.min(2.0, Math.max(0.1, memoryStrength / 10.0));
        user.setMemoryStrength(normalizedStrength);
        userRepository.save(user);
        log.info("Updated memoryStrength for user {} to {}", user.getUsername(), normalizedStrength);

        // Assemble the report with enhanced data
        DiagnosisReport report = new DiagnosisReport();
        report.setMemoryStrength(normalizedStrength);
        report.setForgettingRate(forgettingRate);
        report.setHalfLifeDays(halfLife);
        report.setOptimalDailyWords(optimalDaily);
        report.setMasteryDistribution(masteryDistribution);
        report.setWeakCategories(weakAreas);
        report.setStrongCategories(strongAreas);
        report.setRecommendations(recommendations);
        report.setCurvePoints(curvePoints);
        report.setInsightsText(insights);
        
        // Enhanced fields
        report.setLearningPatterns(learningPatterns);
        report.setMemoryStyle(memoryStyle);
        report.setPredictiveAnalysis(predictiveAnalysis);

        String style = memoryStyle != null && memoryStyle.get("style") instanceof String ? (String) memoryStyle.get("style") : "UNKNOWN";
        log.info("Enhanced diagnosis complete for user {}: S={}, forgettingRate={}, halfLife={} days, style={}",
                user.getUsername(), memoryStrength, forgettingRate, String.format("%.1f", halfLife),
                style);
        return report;
    }

    @Override
    public String getName() {
        return "diagnosis";
    }

    // =========================================================================
    // Enhanced Diagnosis Features
    // =========================================================================

    /**
     * Analyze learning patterns from user's actual session history.
     * All data is dynamically generated based on real user activity.
     * Returns a map containing:
     * - preferredDay: best day for learning (Chinese name)
     * - preferredTimeSlot: most active time slot (Chinese description)
     * - averageSessionDuration: average session length in minutes
     * - weeklyPattern: learning distribution by day of week
     * - hourlyPattern: learning distribution by hour of day
     * - sessionDurationStats: detailed duration statistics
     */
    private Map<String, Object> analyzeLearningPatterns(User user) {
        log.info("Analyzing learning patterns for user: {}", user.getUsername());
        Map<String, Object> patterns = new LinkedHashMap<>();

        List<LearningSession> sessions = learningSessionRepository.findByUserIdSince(user.getId(), 
                LocalDateTime.now().minusDays(30));

        if (sessions.isEmpty()) {
            patterns.put("status", "insufficient_data");
            patterns.put("message", "需要更多学习数据来分析学习模式");
            patterns.put("preferredDay", "暂无数据");
            patterns.put("preferredTimeSlot", "暂无数据");
            patterns.put("averageSessionDuration", "暂无数据");
            return patterns;
        }

        // Analyze session duration
        double avgDuration = sessions.stream()
                .filter(s -> s.getStartedAt() != null && s.getEndedAt() != null)
                .mapToDouble(s -> ChronoUnit.MINUTES.between(s.getStartedAt(), s.getEndedAt()))
                .average()
                .orElse(0.0);

        long maxDuration = sessions.stream()
                .filter(s -> s.getStartedAt() != null && s.getEndedAt() != null)
                .mapToLong(s -> ChronoUnit.MINUTES.between(s.getStartedAt(), s.getEndedAt()))
                .max()
                .orElse(0L);

        long minDuration = sessions.stream()
                .filter(s -> s.getStartedAt() != null && s.getEndedAt() != null)
                .mapToLong(s -> ChronoUnit.MINUTES.between(s.getStartedAt(), s.getEndedAt()))
                .min()
                .orElse(0L);

        Map<String, Object> durationStats = new LinkedHashMap<>();
        durationStats.put("averageMinutes", Math.round(avgDuration * 100.0) / 100.0);
        durationStats.put("maxMinutes", maxDuration);
        durationStats.put("minMinutes", minDuration);
        patterns.put("sessionDurationStats", durationStats);

        // Format average session duration for frontend
        patterns.put("averageSessionDuration", 
                avgDuration >= 60 
                    ? String.format("%.1f小时", avgDuration / 60) 
                    : String.format("%.0f分钟", avgDuration));

        // Analyze weekly pattern (by day of week)
        Map<String, Long> weeklyPattern = new LinkedHashMap<>();
        DayOfWeek[] days = DayOfWeek.values();
        String[] dayNamesCn = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        for (int i = 0; i < days.length; i++) {
            weeklyPattern.put(dayNamesCn[i], 0L);
        }
        for (LearningSession s : sessions) {
            if (s.getStartedAt() != null) {
                int dayIndex = s.getStartedAt().getDayOfWeek().ordinal();
                weeklyPattern.merge(dayNamesCn[dayIndex], 1L, Long::sum);
            }
        }
        patterns.put("weeklyPattern", weeklyPattern);

        // Find preferred day of week (Chinese name)
        String preferredDay = weeklyPattern.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("暂无数据");
        patterns.put("preferredDay", preferredDay);
        patterns.put("preferredDayOfWeek", preferredDay);

        // Analyze hourly pattern (actual hours from data)
        Map<Integer, Long> hourlyPattern = new TreeMap<>();
        for (int h = 0; h < 24; h++) {
            hourlyPattern.put(h, 0L);
        }
        for (LearningSession s : sessions) {
            if (s.getStartedAt() != null) {
                int hour = s.getStartedAt().getHour();
                hourlyPattern.merge(hour, 1L, Long::sum);
            }
        }
        patterns.put("hourlyPattern", hourlyPattern);

        // Find most active hour and format as time slot (Chinese description)
        int mostActiveHour = hourlyPattern.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);
        
        String preferredTimeSlot;
        if (mostActiveHour >= 0) {
            if (mostActiveHour >= 6 && mostActiveHour < 9) {
                preferredTimeSlot = "早晨 " + mostActiveHour + ":00-" + (mostActiveHour + 1) + ":00";
            } else if (mostActiveHour >= 9 && mostActiveHour < 12) {
                preferredTimeSlot = "上午 " + mostActiveHour + ":00-" + (mostActiveHour + 1) + ":00";
            } else if (mostActiveHour >= 12 && mostActiveHour < 14) {
                preferredTimeSlot = "中午 " + mostActiveHour + ":00-" + (mostActiveHour + 1) + ":00";
            } else if (mostActiveHour >= 14 && mostActiveHour < 18) {
                preferredTimeSlot = "下午 " + mostActiveHour + ":00-" + (mostActiveHour + 1) + ":00";
            } else if (mostActiveHour >= 18 && mostActiveHour < 22) {
                preferredTimeSlot = "晚上 " + mostActiveHour + ":00-" + (mostActiveHour + 1) + ":00";
            } else {
                preferredTimeSlot = "深夜 " + mostActiveHour + ":00-" + (mostActiveHour + 1) + ":00";
            }
        } else {
            preferredTimeSlot = "暂无数据";
        }
        patterns.put("preferredTimeSlot", preferredTimeSlot);
        patterns.put("mostActiveHour", mostActiveHour);

        patterns.put("status", "success");
        patterns.put("sampleSize", sessions.size());

        log.info("Learning pattern analysis complete for user {}: preferredDay={}, preferredTimeSlot={}, avgDuration={}min",
                user.getUsername(), preferredDay, preferredTimeSlot, String.format("%.1f", avgDuration));
        return patterns;
    }

    /**
     * Identify memory style/type based on learning patterns and review performance.
     * Possible types: VISUAL, AUDITORY, SEMANTIC, KINESTHETIC, MIXED
     */
    private Map<String, Object> identifyMemoryStyle(User user, List<UserWord> userWords, List<ReviewRecord> reviews) {
        log.info("Identifying memory style for user: {}", user.getUsername());
        Map<String, Object> memoryStyle = new LinkedHashMap<>();

        if (reviews.size() < 20) {
            memoryStyle.put("style", "MIXED");
            memoryStyle.put("confidence", 0.3);
            memoryStyle.put("reason", "数据不足，默认混合学习类型");
            memoryStyle.put("characteristics", List.of("需要更多学习数据来确定学习类型"));
            return memoryStyle;
        }

        // Analyze word type performance
        Map<String, Double> typePerformance = new LinkedHashMap<>();
        
        // Performance by difficulty level
        Map<Integer, Double> difficultyAccuracy = new HashMap<>();
        for (ReviewRecord r : reviews) {
            UserWord uw = userWordRepository.findById(r.getUserWordId()).orElse(null);
            if (uw != null && uw.getWord() != null && uw.getWord().getDifficulty() != null) {
                int diff = uw.getWord().getDifficulty();
                difficultyAccuracy.merge(diff, r.getCorrect() ? 1.0 : 0.0, Double::sum);
            }
        }

        // Analyze performance by word length (shorter words may indicate visual learners)
        Map<String, Double> lengthPerformance = new HashMap<>();
        lengthPerformance.put("short", 0.0);
        lengthPerformance.put("medium", 0.0);
        lengthPerformance.put("long", 0.0);
        Map<String, Integer> lengthCounts = new HashMap<>();
        lengthCounts.put("short", 0);
        lengthCounts.put("medium", 0);
        lengthCounts.put("long", 0);

        for (ReviewRecord r : reviews) {
            UserWord uw = userWordRepository.findById(r.getUserWordId()).orElse(null);
            if (uw != null && uw.getWord() != null) {
                int len = uw.getWord().getWord().length();
                String category;
                if (len <= 4) category = "short";
                else if (len <= 7) category = "medium";
                else category = "long";
                
                lengthPerformance.merge(category, r.getCorrect() ? 1.0 : 0.0, Double::sum);
                lengthCounts.merge(category, 1, Integer::sum);
            }
        }

        // Calculate accuracy rates
        Map<String, Double> lengthAccuracy = new LinkedHashMap<>();
        for (String cat : lengthCounts.keySet()) {
            if (lengthCounts.get(cat) > 0) {
                lengthAccuracy.put(cat, lengthPerformance.get(cat) / lengthCounts.get(cat));
            } else {
                lengthAccuracy.put(cat, 0.0);
            }
        }

        // Determine memory style based on patterns
        String style = "MIXED";
        double confidence = 0.5;
        List<String> characteristics = new ArrayList<>();

        // Visual learners: better with shorter words, high accuracy on first exposure
        double shortAccuracy = lengthAccuracy.getOrDefault("short", 0.0);
        double longAccuracy = lengthAccuracy.getOrDefault("long", 0.0);
        double wordLengthDiff = shortAccuracy - longAccuracy;

        // Semantic learners: better with higher difficulty (more complex words)
        double avgDifficultyAccuracy = difficultyAccuracy.entrySet().stream()
                .filter(e -> e.getKey() >= 3)
                .mapToDouble(Map.Entry::getValue)
                .average()
                .orElse(0.0);

        // Analyze review speed patterns
        long fastReviews = reviews.stream()
                .filter(r -> r.getResponseTime() != null && r.getResponseTime() < 3000)
                .count();
        double fastReviewRatio = reviews.size() > 0 ? (double) fastReviews / reviews.size() : 0.0;

        // Determine style
        if (wordLengthDiff > 0.15 && shortAccuracy > 0.6) {
            style = "VISUAL";
            confidence = 0.6 + wordLengthDiff * 0.5;
            characteristics.add("擅长记忆短单词，视觉识别能力强");
            characteristics.add("建议使用图像联想和单词卡片");
        } else if (avgDifficultyAccuracy > 0.6 && longAccuracy > shortAccuracy) {
            style = "SEMANTIC";
            confidence = 0.6 + (avgDifficultyAccuracy - 0.6);
            characteristics.add("擅长理解和记忆复杂单词");
            characteristics.add("建议使用例句和语境学习");
        } else if (fastReviewRatio > 0.6) {
            style = "AUDITORY";
            confidence = 0.5 + fastReviewRatio * 0.3;
            characteristics.add("反应速度快，可能善于听觉记忆");
            characteristics.add("建议多听单词发音和例句");
        } else {
            style = "MIXED";
            confidence = 0.5;
            characteristics.add("学习模式均衡，适合多种学习方法");
            characteristics.add("建议尝试不同的记忆技巧");
        }

        confidence = Math.min(1.0, Math.max(0.3, confidence));

        memoryStyle.put("style", style);
        memoryStyle.put("confidence", Math.round(confidence * 1000.0) / 1000.0);
        memoryStyle.put("characteristics", characteristics);
        memoryStyle.put("wordLengthPerformance", lengthAccuracy);
        memoryStyle.put("difficultyPerformance", difficultyAccuracy);
        memoryStyle.put("fastReviewRatio", Math.round(fastReviewRatio * 1000.0) / 1000.0);

        log.info("Memory style identified for user {}: style={}, confidence={}",
                user.getUsername(), style, String.format("%.2f", confidence));
        return memoryStyle;
    }

    /**
     * Generate comprehensive predictive analysis based on user's actual learning data.
     * All predictions are dynamically computed from real user activity, not fixed values.
     * Includes: 7-day retention prediction, high-risk word identification, 
     * estimated mastery time, and review planning.
     */
    private Map<String, Object> generatePredictiveAnalysis(User user, double memoryStrength, 
            List<UserWord> userWords) {
        log.info("Generating predictive analysis for user: {}", user.getUsername());
        Map<String, Object> prediction = new LinkedHashMap<>();

        LocalDateTime now = LocalDateTime.now();

        double forgettingRate = BASE_DECAY_CONSTANT_LAMBDA / memoryStrength;

        // Predict forgetting over next 30 days with detailed points
        List<Map<String, Object>> forgettingPrediction = new ArrayList<>();
        for (int days = 1; days <= 30; days += 3) {
            double predictedRetention = Math.exp(-forgettingRate * days);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("days", days);
            point.put("predictedRetention", Math.round(predictedRetention * 10000.0) / 10000.0);
            point.put("predictedForgetRate", Math.round((1 - predictedRetention) * 10000.0) / 10000.0);
            forgettingPrediction.add(point);
        }
        prediction.put("forgettingPrediction", forgettingPrediction);

        // Key retention predictions using SRS formula R = e^(-d*t)
        Map<String, Object> keyRetentions = new LinkedHashMap<>();
        double oneDayRetention = Math.round(Math.exp(-forgettingRate * 1.0) * 10000.0) / 10000.0;
        double threeDayRetention = Math.round(Math.exp(-forgettingRate * 3.0) * 10000.0) / 10000.0;
        double sevenDayRetention = Math.round(Math.exp(-forgettingRate * 7.0) * 10000.0) / 10000.0;
        double fourteenDayRetention = Math.round(Math.exp(-forgettingRate * 14.0) * 10000.0) / 10000.0;
        double thirtyDayRetention = Math.round(Math.exp(-forgettingRate * 30.0) * 10000.0) / 10000.0;
        
        keyRetentions.put("oneDayRetention", oneDayRetention);
        keyRetentions.put("threeDayRetention", threeDayRetention);
        keyRetentions.put("sevenDayRetention", sevenDayRetention);
        keyRetentions.put("fourteenDayRetention", fourteenDayRetention);
        keyRetentions.put("thirtyDayRetention", thirtyDayRetention);
        prediction.put("keyRetentions", keyRetentions);
        prediction.put("memoryStrength", Math.round(memoryStrength * 1000.0) / 1000.0);
        prediction.put("forgettingRate", Math.round(forgettingRate * 10000.0) / 10000.0);

        // High risk words analysis based on actual user word data
        Map<String, Object> riskAnalysis = analyzeHighRiskWords(userWords, memoryStrength, now);
        prediction.put("highRiskWords", riskAnalysis);

        // Estimated mastery time based on actual learning progress
        Map<String, Object> masteryTime = estimateMasteryTime(user, userWords);
        prediction.put("masteryTimeEstimation", masteryTime);

        // Estimate words that will need review in each time period
        Map<String, Long> reviewNeed = new LinkedHashMap<>();
        
        long mastered = userWords.stream().filter(uw -> "mastered".equals(uw.getStatus())).count();
        long nonMastered = userWords.size() - mastered;
        
        // Words needing review in next 3 days
        long urgent = userWords.stream()
                .filter(uw -> uw.getNextReview() != null && uw.getNextReview().isBefore(now.plusDays(3)))
                .filter(uw -> !"mastered".equals(uw.getStatus()))
                .count();
        reviewNeed.put("urgent_0_3_days", urgent);

        // Words needing review in next 7 days
        long soon = userWords.stream()
                .filter(uw -> uw.getNextReview() != null && uw.getNextReview().isBefore(now.plusDays(7)))
                .filter(uw -> !"mastered".equals(uw.getStatus()))
                .count();
        reviewNeed.put("soon_3_7_days", soon - urgent);

        // Words needing review in next 14 days
        long upcoming = userWords.stream()
                .filter(uw -> uw.getNextReview() != null && uw.getNextReview().isBefore(now.plusDays(14)))
                .filter(uw -> !"mastered".equals(uw.getStatus()))
                .count();
        reviewNeed.put("upcoming_7_14_days", upcoming - soon);

        // Words needing review in next 30 days
        long later = userWords.stream()
                .filter(uw -> uw.getNextReview() != null && uw.getNextReview().isBefore(now.plusDays(30)))
                .filter(uw -> !"mastered".equals(uw.getStatus()))
                .count();
        reviewNeed.put("later_14_30_days", later - upcoming);

        prediction.put("reviewNeedByPeriod", reviewNeed);
        prediction.put("totalNonMasteredWords", nonMastered);
        prediction.put("totalMasteredWords", mastered);

        // Estimated effort needed with detailed breakdown
        Map<String, Object> effort = new LinkedHashMap<>();
        double avgReviewTime = 0.5;
        double avgNewWordTime = 2.0;
        int dailyNewWords = user.getDailyNewWords() != null ? user.getDailyNewWords() : 20;
        
        double totalReviewTimeMinutes = later * avgReviewTime;
        double totalNewWordTimeMinutes = dailyNewWords * avgNewWordTime;
        
        effort.put("estimatedTotalReviews", later);
        effort.put("estimatedReviewTimeMinutes", Math.round(totalReviewTimeMinutes));
        effort.put("estimatedDailyReviews", Math.round(later / 30.0));
        effort.put("estimatedDailyReviewMinutes", Math.round(totalReviewTimeMinutes / 30.0));
        effort.put("estimatedDailyNewWordMinutes", Math.round(totalNewWordTimeMinutes));
        effort.put("estimatedDailyTotalMinutes", Math.round(totalReviewTimeMinutes / 30.0 + totalNewWordTimeMinutes));
        prediction.put("estimatedEffort", effort);

        // Warning thresholds
        Map<String, Object> warnings = new LinkedHashMap<>();
        List<String> warningList = new ArrayList<>();
        
        if (urgent > nonMastered * 0.3 && nonMastered > 0) {
            warningList.add("超过30%的未掌握单词即将过期，建议优先复习");
        }
        if (sevenDayRetention < 0.5) {
            warningList.add("7日预测留存率低于50%，建议增加复习频率");
        }
        long highRiskCount = riskAnalysis.get("highRiskCount") instanceof Long 
                ? (Long) riskAnalysis.get("highRiskCount") 
                : 0;
        if (highRiskCount > 10) {
            warningList.add("存在较多高遗忘风险单词，需要重点关注");
        }
        
        warnings.put("hasWarnings", !warningList.isEmpty());
        warnings.put("warnings", warningList);
        prediction.put("warnings", warnings);

        Long estimatedDays = masteryTime.get("estimatedDaysToMasterCurrent") instanceof Long 
                ? (Long) masteryTime.get("estimatedDaysToMasterCurrent") 
                : 0L;
        
        // Frontend-compatible field names
        prediction.put("expectedRetention", sevenDayRetention);
        prediction.put("riskWordsCount", highRiskCount);
        prediction.put("estimatedMasteryDays", estimatedDays);
        
        // Generate suggestion based on actual data
        String suggestion = generatePredictionSuggestion(sevenDayRetention, highRiskCount, 
                estimatedDays, nonMastered);
        prediction.put("suggestion", suggestion);

        log.info("Predictive analysis complete for user {}: 7-day retention={}%, {} high risk, {} days to master",
                user.getUsername(), String.format("%.0f", sevenDayRetention * 100), highRiskCount, 
                estimatedDays);
        return prediction;
    }

    /**
     * Generate personalized suggestion based on prediction results.
     */
    private String generatePredictionSuggestion(double sevenDayRetention, long highRiskCount, 
            Long estimatedDays, long nonMastered) {
        StringBuilder sb = new StringBuilder();
        
        if (sevenDayRetention < 0.5) {
            sb.append("您的记忆留存率较低，建议缩短复习间隔并增加复习频率。");
        } else if (sevenDayRetention < 0.7) {
            sb.append("您的记忆留存率处于中等水平，建议定期复习已学单词。");
        } else {
            sb.append("您的记忆留存率良好，继续保持当前学习节奏。");
        }
        
        if (highRiskCount > 5) {
            sb.append(" 当前有").append(highRiskCount).append("个单词存在较高遗忘风险，需要优先复习。");
        }
        
        if (estimatedDays != null && estimatedDays > 0) {
            sb.append(" 预计还需要").append(estimatedDays).append("天可以掌握当前学习的词汇。");
        }
        
        return sb.toString();
    }

    /**
     * Analyze words at high risk of forgetting based on current mastery, retrievability,
     * and predicted retention.
     */
    private Map<String, Object> analyzeHighRiskWords(List<UserWord> userWords, double stability, 
            LocalDateTime now) {
        Map<String, Object> riskAnalysis = new LinkedHashMap<>();
        
        List<Map<String, Object>> highRiskList = new ArrayList<>();
        List<Map<String, Object>> mediumRiskList = new ArrayList<>();
        
        for (UserWord uw : userWords) {
            if ("mastered".equals(uw.getStatus())) continue;
            
            double mastery = uw.getMasteryLevel() != null ? uw.getMasteryLevel() : 0.0;
            double retrievability = uw.getMemoryRetrievability() != null ? uw.getMemoryRetrievability() : 1.0;
            
            // Calculate risk score based on multiple factors
            double riskScore = 0.0;
            
            // Mastery deficit (higher risk if low mastery)
            riskScore += (1.0 - mastery) * 40;
            
            // Retrievability (higher risk if low retrievability)
            riskScore += (1.0 - retrievability) * 30;
            
            // Overdue status (higher risk if overdue)
            if (uw.getNextReview() != null && uw.getNextReview().isBefore(now)) {
                long hoursOverdue = ChronoUnit.HOURS.between(uw.getNextReview(), now);
                riskScore += Math.min(hoursOverdue / 24.0 * 20, 20);
            }
            
            // Error rate (higher risk if frequent errors)
            int reviewCount = uw.getReviewCount() != null ? uw.getReviewCount() : 0;
            int wrongCount = uw.getWrongCount() != null ? uw.getWrongCount() : 0;
            double errorRate = reviewCount > 0 ? (double) wrongCount / reviewCount : 0.0;
            riskScore += errorRate * 10;
            
            String riskLevel;
            if (riskScore >= 60) riskLevel = "high";
            else if (riskScore >= 30) riskLevel = "medium";
            else riskLevel = "low";
            
            if ("high".equals(riskLevel) || "medium".equals(riskLevel)) {
                Map<String, Object> wordRisk = new LinkedHashMap<>();
                wordRisk.put("userWordId", uw.getId());
                wordRisk.put("wordId", uw.getWordId());
                wordRisk.put("masteryLevel", mastery);
                wordRisk.put("retrievability", retrievability);
                wordRisk.put("riskScore", Math.round(riskScore * 100.0) / 100.0);
                wordRisk.put("riskLevel", riskLevel);
                
                if (uw.getWord() != null) {
                    wordRisk.put("word", uw.getWord().getWord());
                    wordRisk.put("definitionCn", uw.getWord().getDefinitionCn());
                }
                
                if ("high".equals(riskLevel)) {
                    highRiskList.add(wordRisk);
                } else {
                    mediumRiskList.add(wordRisk);
                }
            }
        }
        
        // Sort by risk score descending
        highRiskList.sort((a, b) -> {
            Double ra = a.get("riskScore") instanceof Double ? (Double) a.get("riskScore") : 0.0;
            Double rb = b.get("riskScore") instanceof Double ? (Double) b.get("riskScore") : 0.0;
            return Double.compare(rb, ra);
        });
        mediumRiskList.sort((a, b) -> {
            Double ra = a.get("riskScore") instanceof Double ? (Double) a.get("riskScore") : 0.0;
            Double rb = b.get("riskScore") instanceof Double ? (Double) b.get("riskScore") : 0.0;
            return Double.compare(rb, ra);
        });
        
        riskAnalysis.put("highRiskCount", (long) highRiskList.size());
        riskAnalysis.put("mediumRiskCount", (long) mediumRiskList.size());
        riskAnalysis.put("totalAtRisk", (long) (highRiskList.size() + mediumRiskList.size()));
        riskAnalysis.put("highRiskWords", highRiskList.stream().limit(15).collect(Collectors.toList()));
        riskAnalysis.put("mediumRiskWords", mediumRiskList.stream().limit(10).collect(Collectors.toList()));
        
        return riskAnalysis;
    }

    /**
     * Estimate time needed to master current learning goals based on current progress.
     */
    private Map<String, Object> estimateMasteryTime(User user, List<UserWord> userWords) {
        Map<String, Object> masteryTime = new LinkedHashMap<>();
        
        long mastered = userWords.stream().filter(uw -> "mastered".equals(uw.getStatus())).count();
        long learning = userWords.stream().filter(uw -> "learning".equals(uw.getStatus())).count();
        long reviewing = userWords.stream().filter(uw -> "review".equals(uw.getStatus())).count();
        long nonMastered = learning + reviewing;
        
        // Calculate current learning rate
        LocalDateTime earliestLearn = userWords.stream()
                .filter(uw -> uw.getLearnDate() != null)
                .map(UserWord::getLearnDate)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        
        long daysSinceStart = Math.max(1, ChronoUnit.DAYS.between(earliestLearn, LocalDateTime.now()));
        double wordsPerDay = (double) userWords.size() / daysSinceStart;
        
        // Estimate days to master current non-mastered words
        double avgMasteryProgress = userWords.stream()
                .filter(uw -> !"mastered".equals(uw.getStatus()))
                .mapToDouble(uw -> uw.getMasteryLevel() != null ? uw.getMasteryLevel() : 0.0)
                .average()
                .orElse(0.0);
        
        // Assuming 0.1 mastery increase per successful review cycle
        double estimatedDaysToMasterCurrent = nonMastered > 0 
                ? Math.ceil((nonMastered * (1.0 - avgMasteryProgress) / 0.1) / wordsPerDay)
                : 0;
        
        // Estimate days to reach target vocabulary
        int targetVocab = user.getTargetVocabSize() != null ? user.getTargetVocabSize() : 6000;
        int remainingToTarget = Math.max(0, targetVocab - userWords.size());
        double estimatedDaysToTarget = wordsPerDay > 0 
                ? Math.ceil((double) remainingToTarget / wordsPerDay) + estimatedDaysToMasterCurrent
                : 0;
        
        masteryTime.put("currentMasteredCount", mastered);
        masteryTime.put("currentNonMasteredCount", nonMastered);
        masteryTime.put("currentLearningRate", Math.round(wordsPerDay * 100.0) / 100.0);
        masteryTime.put("estimatedDaysToMasterCurrent", (long) estimatedDaysToMasterCurrent);
        masteryTime.put("targetVocabulary", targetVocab);
        masteryTime.put("remainingToTarget", remainingToTarget);
        masteryTime.put("estimatedDaysToTarget", (long) estimatedDaysToTarget);
        
        // Target date
        LocalDateTime targetDate = LocalDateTime.now().plusDays((long) estimatedDaysToTarget);
        masteryTime.put("estimatedTargetDate", targetDate.toString());
        
        // Progress percentage
        double progressToTarget = userWords.size() > 0 && targetVocab > 0 
                ? (double) userWords.size() / targetVocab
                : 0;
        masteryTime.put("progressToTarget", Math.round(progressToTarget * 10000.0) / 10000.0);
        
        return masteryTime;
    }

    // =========================================================================
    // Forgetting Curve Fitting
    // =========================================================================

    /**
     * Calculate memory strength using the SRS formula:
     * S_n = S_{n-1} × e^[α(Q-3)] × (1 - β × D)
     * 
     * Where:
     * - S_n: Current memory strength after n-th review
     * - S_{n-1}: Previous memory strength (initial = 1.0)
     * - α: Quality weight parameter (0.5)
     * - Q: Recall quality score (0-5 scale, 3 is passing)
     * - β: Difficulty weight parameter (0.15)
     * - D: Word difficulty coefficient
     * 
     * @param user the user
     * @param userWords user's word records
     * @param reviews review records
     * @return computed memory strength value
     */
    private double calculateMemoryStrength(User user, List<UserWord> userWords, List<ReviewRecord> reviews) {
        if (reviews.isEmpty()) {
            return INITIAL_STRENGTH;
        }

        Map<Long, UserWord> userWordMap = userWords.stream()
                .collect(Collectors.toMap(UserWord::getId, uw -> uw));

        Map<Long, Double> wordStrengths = new LinkedHashMap<>();
        Map<Long, Integer> wordReviewCounts = new HashMap<>();

        for (ReviewRecord review : reviews) {
            Long userWordId = review.getUserWordId();
            if (userWordId == null) continue;

            UserWord uw = userWordMap.get(userWordId);
            if (uw == null || uw.getWord() == null) continue;

            Integer quality = review.getQuality();
            int Q;
            if (quality != null) {
                Q = quality;
            } else {
                Q = Boolean.TRUE.equals(review.getCorrect()) ? 5 : 2;
            }

            int difficulty = uw.getWord().getDifficulty() != null ? uw.getWord().getDifficulty() : 3;
            double D = Math.max(0.1, Math.min(1.0, difficulty / 5.0));

            double qualityFactor = Math.exp(QUALITY_WEIGHT_ALPHA * (Q - 3));
            double difficultyFactor = (1 - DIFFICULTY_WEIGHT_BETA * D);

            double prevStrength = wordStrengths.getOrDefault(userWordId, INITIAL_STRENGTH);
            double newStrength = prevStrength * qualityFactor * difficultyFactor;

            newStrength = Math.max(0.1, Math.min(10.0, newStrength));

            wordStrengths.put(userWordId, newStrength);
            wordReviewCounts.merge(userWordId, 1, Integer::sum);
        }

        if (wordStrengths.isEmpty()) {
            return INITIAL_STRENGTH;
        }

        double totalWeightedStrength = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<Long, Double> entry : wordStrengths.entrySet()) {
            int reviewCount = wordReviewCounts.getOrDefault(entry.getKey(), 1);
            totalWeightedStrength += entry.getValue() * reviewCount;
            totalWeight += reviewCount;
        }

        double strength = totalWeight > 0 ? totalWeightedStrength / totalWeight : INITIAL_STRENGTH;
        strength = Math.max(0.5, Math.min(20.0, strength));

        log.info("Memory strength calculation: {} words, avg strength={}", wordStrengths.size(), 
                String.format("%.2f", strength));
        return strength;
    }

    /**
     * Find the best matching bin index for a given interval.
     * Returns the index of the nearest bin, or -1 if the interval is out of range.
     */
    private int findBinIndex(int interval) {
        if (interval < 1 || interval > 45) {
            return -1;
        }

        int bestIndex = 0;
        int bestDist = Math.abs(INTERVAL_BINS[0] - interval);
        for (int i = 1; i < INTERVAL_BINS.length; i++) {
            int dist = Math.abs(INTERVAL_BINS[i] - interval);
            if (dist < bestDist) {
                bestDist = dist;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    /**
     * Build curve visualization points comparing predicted vs actual retention.
     * Uses the SRS formula R = e^(-d*t) where d = λ/S.
     */
    private List<ForgettingCurvePoint> buildCurvePoints(double memoryStrength, List<ReviewRecord> reviews) {
        Map<Integer, Double> actualByBin = computeActualRetentionByBin(reviews);

        double forgettingRate = BASE_DECAY_CONSTANT_LAMBDA / memoryStrength;

        List<ForgettingCurvePoint> points = new ArrayList<>();
        for (int day : CURVE_DAYS) {
            ForgettingCurvePoint point = new ForgettingCurvePoint();
            point.setDaysSinceReview(day);

            double predicted = Math.exp(-forgettingRate * day);
            point.setPredictedRetention(Math.round(predicted * 10000.0) / 10000.0);

            Double actual = actualByBin.get(day);
            if (actual != null) {
                point.setActualRetention(Math.round(actual * 10000.0) / 10000.0);
            } else {
                point.setActualRetention(null);
            }

            point.setMemoryStrength(Math.round(memoryStrength * 1000.0) / 1000.0);
            point.setForgettingRate(Math.round(forgettingRate * 10000.0) / 10000.0);

            points.add(point);
        }
        return points;
    }

    /**
     * Compute actual retention rate for each interval bin.
     */
    private Map<Integer, Double> computeActualRetentionByBin(List<ReviewRecord> reviews) {
        Map<Integer, int[]> binStats = new HashMap<>();

        for (ReviewRecord review : reviews) {
            Integer prevInterval = review.getPrevInterval();
            if (prevInterval == null || prevInterval <= 0) {
                continue;
            }
            int binIndex = findBinIndex(prevInterval);
            if (binIndex < 0) {
                continue;
            }

            int binDay = INTERVAL_BINS[binIndex];
            int[] stats = binStats.computeIfAbsent(binDay, k -> new int[2]);
            stats[0]++; // total
            if (Boolean.TRUE.equals(review.getCorrect())) {
                stats[1]++; // correct
            }
        }

        Map<Integer, Double> result = new HashMap<>();
        for (Map.Entry<Integer, int[]> entry : binStats.entrySet()) {
            int[] stats = entry.getValue();
            if (stats[0] >= 3) {
                result.put(entry.getKey(), (double) stats[1] / stats[0]);
            }
        }
        return result;
    }

    // =========================================================================
    // Mastery Analysis
    // =========================================================================

    /**
     * Analyzes the user's mastery distribution by status and mastery bands.
     * Enhanced with memory strength based metrics using SRS formulas.
     *
     * @param userWords all words for the user
     * @return a map containing status distribution and mastery band distribution
     */
    private Map<String, Object> analyzeMastery(List<UserWord> userWords) {
        Map<String, Object> distribution = new LinkedHashMap<>();

        // --- Status distribution ---
        Map<String, Long> statusCounts = userWords.stream()
                .collect(Collectors.groupingBy(UserWord::getStatus, Collectors.counting()));
        distribution.put("statusDistribution", statusCounts);

        // --- Mastery band distribution ---
        Map<String, Long> masteryBands = new LinkedHashMap<>();
        for (String label : MASTERY_BAND_LABELS) {
            masteryBands.put(label, 0L);
        }

        // --- Memory strength based analysis ---
        double totalMemoryStrength = 0.0;
        double totalRetentionPotential = 0.0;
        int wordsWithStrength = 0;

        Map<String, Long> strengthBands = new LinkedHashMap<>();
        strengthBands.put("0-1.0", 0L);
        strengthBands.put("1.0-2.0", 0L);
        strengthBands.put("2.0-3.0", 0L);
        strengthBands.put("3.0+", 0L);

        for (UserWord uw : userWords) {
            double mastery = uw.getMasteryLevel() != null ? uw.getMasteryLevel() : 0.0;
            String band = getMasteryBand(mastery);
            masteryBands.merge(band, 1L, Long::sum);

            double strength = uw.getMemoryStability() != null ? uw.getMemoryStability() : INITIAL_STRENGTH;
            totalMemoryStrength += strength;
            double forgettingRate = BASE_DECAY_CONSTANT_LAMBDA / strength;
            totalRetentionPotential += Math.exp(-forgettingRate * 7);
            wordsWithStrength++;

            if (strength < 1.0) {
                strengthBands.merge("0-1.0", 1L, Long::sum);
            } else if (strength < 2.0) {
                strengthBands.merge("1.0-2.0", 1L, Long::sum);
            } else if (strength < 3.0) {
                strengthBands.merge("2.0-3.0", 1L, Long::sum);
            } else {
                strengthBands.merge("3.0+", 1L, Long::sum);
            }
        }
        distribution.put("masteryBands", masteryBands);
        distribution.put("memoryStrengthBands", strengthBands);

        // --- Summary statistics ---
        double totalWords = userWords.size();
        long masteredCount = userWords.stream()
                .filter(uw -> "mastered".equals(uw.getStatus()))
                .count();
        long learningCount = userWords.stream()
                .filter(uw -> "learning".equals(uw.getStatus()) || "new".equals(uw.getStatus()))
                .count();
        long reviewCount = userWords.stream()
                .filter(uw -> "review".equals(uw.getStatus()))
                .count();

        double avgMastery = userWords.stream()
                .mapToDouble(uw -> uw.getMasteryLevel() != null ? uw.getMasteryLevel() : 0.0)
                .average()
                .orElse(0.0);

        double avgRetrievability = userWords.stream()
                .filter(uw -> !"mastered".equals(uw.getStatus()) && !"forgotten".equals(uw.getStatus()))
                .mapToDouble(uw -> uw.getMemoryRetrievability() != null ? uw.getMemoryRetrievability() : 0.0)
                .average()
                .orElse(0.0);

        double avgMemoryStrength = wordsWithStrength > 0 ? totalMemoryStrength / wordsWithStrength : INITIAL_STRENGTH;
        double avg7DayRetention = wordsWithStrength > 0 ? totalRetentionPotential / wordsWithStrength : 0.5;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalWords", (long) totalWords);
        summary.put("masteredCount", masteredCount);
        summary.put("learningCount", learningCount);
        summary.put("reviewCount", reviewCount);
        summary.put("avgMastery", Math.round(avgMastery * 10000.0) / 10000.0);
        summary.put("avgRetrievability", Math.round(avgRetrievability * 10000.0) / 10000.0);
        summary.put("masteryRate", totalWords > 0
                ? Math.round(masteredCount / totalWords * 10000.0) / 10000.0
                : 0.0);
        summary.put("avgMemoryStrength", Math.round(avgMemoryStrength * 1000.0) / 1000.0);
        summary.put("avg7DayRetention", Math.round(avg7DayRetention * 10000.0) / 10000.0);

        distribution.put("summary", summary);

        log.info("Mastery analysis: {} words, {} mastered, avgMastery={}, avgMemoryStrength={}",
                (long) totalWords, masteredCount, String.format("%.2f", avgMastery),
                String.format("%.2f", avgMemoryStrength));
        return distribution;
    }

    /**
     * Map a mastery value (0.0 - 1.0) to its band label.
     */
    private String getMasteryBand(double mastery) {
        mastery = Math.max(0.0, Math.min(1.0, mastery));
        for (int i = 0; i < MASTERY_BANDS.length - 1; i++) {
            if (mastery < MASTERY_BANDS[i + 1]) {
                return MASTERY_BAND_LABELS[i];
            }
        }
        return MASTERY_BAND_LABELS[MASTERY_BAND_LABELS.length - 1];
    }

    // =========================================================================
    // Capacity Estimation
    // =========================================================================

    /**
     * Estimates the optimal daily new word count using cognitive load formula:
     * 
     * V = max(0, floor[(C_max - (N_review × W_r)) / W_n]) × γ(S_avg)
     * 
     * Where:
     * - C_max: Maximum daily cognitive load (122)
     * - N_review: Number of words due for review today
     * - W_r: Cognitive weight for review words (1.0)
     * - W_n: Cognitive weight for new words (2.0)
     * - γ(S_avg): State adjustment factor based on average memory strength
     * 
     * @param user    the user
     * @param reviews all review records (chronologically ordered)
     * @return recommended daily new word count
     */
    private int estimateCapacity(User user, List<ReviewRecord> reviews) {
        int currentDaily = user.getDailyNewWords() != null ? user.getDailyNewWords() : 20;

        if (reviews.isEmpty()) {
            return currentDaily;
        }

        List<UserWord> userWords = userWordRepository.findByUserId(user.getId());

        // Calculate N_review: number of words due for review today
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        long reviewCount = userWords.stream()
                .filter(uw -> uw.getNextReview() != null && uw.getNextReview().isBefore(todayEnd))
                .filter(uw -> !"mastered".equals(uw.getStatus()))
                .count();

        // Calculate available cognitive load for new words
        double usedLoad = reviewCount * REVIEW_COGNITIVE_WEIGHT;
        double availableLoad = MAX_COGNITIVE_LOAD - usedLoad;

        // Calculate base recommendation
        int baseVocabulary = (int) Math.floor(availableLoad / NEW_WORD_COGNITIVE_WEIGHT);
        baseVocabulary = Math.max(0, baseVocabulary);

        // Calculate state adjustment factor γ(S_avg) based on memory stability
        double avgStrength = userWords.stream()
                .mapToDouble(uw -> uw.getMemoryStability() != null ? uw.getMemoryStability() : INITIAL_STRENGTH)
                .average()
                .orElse(INITIAL_STRENGTH);

        double stateFactor = 0.8 + (avgStrength - INITIAL_STRENGTH) * 0.15;
        stateFactor = Math.max(0.5, Math.min(1.5, stateFactor));

        // Apply state adjustment
        int optimal = (int) Math.round(baseVocabulary * stateFactor);

        double accuracy = 0.0;
        double multiplier = 1.0;

        // Fallback to accuracy-based adjustment if base is too low
        if (optimal <= 5) {
            int windowSize = Math.min(reviews.size(), 50);
            List<ReviewRecord> recentReviews = reviews.subList(reviews.size() - windowSize, reviews.size());
            long correctCount = recentReviews.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getCorrect()))
                    .count();
            accuracy = (double) correctCount / recentReviews.size();
            
            multiplier = accuracy > 0.85 ? 1.2 : (accuracy > 0.70 ? 1.0 : (accuracy > 0.50 ? 0.8 : 0.6));
            optimal = (int) Math.round(currentDaily * multiplier);
        }

        // Clamp to reasonable range [5, 50]
        optimal = Math.max(5, Math.min(50, optimal));

        log.info("Capacity estimation: accuracy={}, multiplier={}, current={}, optimal={}",
                String.format("%.2f", accuracy), multiplier, currentDaily, optimal);
        return optimal;
    }

    // =========================================================================
    // Area Identification
    // =========================================================================

    /**
     * Identifies weak and strong learning areas based on word categories.
     *
     * Weak areas: categories where average mastery < 30% or where average
     * retrievability is declining (< 0.5).
     *
     * Strong areas: categories where average mastery >= 70%.
     *
     * @param userWords all user words
     * @return map with "weak" and "strong" lists of category names
     */
    private Map<String, List<String>> identifyAreas(List<UserWord> userWords) {
        // Group words by part of speech as the primary category dimension
        Map<String, List<UserWord>> byCategory = new LinkedHashMap<>();

        for (UserWord uw : userWords) {
            String category = "unknown";
            try {
                if (uw.getWord() != null && uw.getWord().getPartOfSpeech() != null) {
                    category = uw.getWord().getPartOfSpeech();
                }
            } catch (Exception e) {
                // Lazy loading may fail if session is detached; fall back to "unknown"
                log.debug("Could not load word details for userWord {}: {}", uw.getId(), e.getMessage());
            }
            byCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(uw);
        }

        // Also try to extract tag-based categories from Word.tags JSON
        Map<String, List<UserWord>> byTag = new LinkedHashMap<>();
        for (UserWord uw : userWords) {
            try {
                if (uw.getWord() != null && uw.getWord().getTags() != null) {
                    String tagsJson = uw.getWord().getTags();
                    // Simple JSON array parsing: ["cet4","cet6"]
                    String cleaned = tagsJson.replaceAll("[\\[\\]\"]", "");
                    if (!cleaned.isBlank()) {
                        for (String tag : cleaned.split(",")) {
                            String trimmed = tag.trim();
                            if (!trimmed.isEmpty()) {
                                byTag.computeIfAbsent(trimmed, k -> new ArrayList<>()).add(uw);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Could not parse tags for userWord {}: {}", uw.getId(), e.getMessage());
            }
        }

        // Evaluate categories
        List<String> weakAreas = new ArrayList<>();
        List<String> strongAreas = new ArrayList<>();

        evaluateAreaGroups(byCategory, weakAreas, strongAreas);
        evaluateAreaGroups(byTag, weakAreas, strongAreas);

        // Deduplicate
        weakAreas = weakAreas.stream().distinct().collect(Collectors.toList());
        strongAreas = strongAreas.stream().distinct().collect(Collectors.toList());

        log.info("Identified {} weak areas and {} strong areas", weakAreas.size(), strongAreas.size());

        Map<String, List<String>> result = new HashMap<>();
        result.put("weak", weakAreas);
        result.put("strong", strongAreas);
        return result;
    }

    /**
     * Evaluate a group of categorized words and classify each category as
     * weak or strong.
     */
    private void evaluateAreaGroups(Map<String, List<UserWord>> categorized,
                                    List<String> weakAreas,
                                    List<String> strongAreas) {
        for (Map.Entry<String, List<UserWord>> entry : categorized.entrySet()) {
            String category = entry.getKey();
            List<UserWord> words = entry.getValue();

            if (words.size() < 2) {
                continue; // skip categories with too few words
            }

            double avgMastery = words.stream()
                    .mapToDouble(uw -> uw.getMasteryLevel() != null ? uw.getMasteryLevel() : 0.0)
                    .average()
                    .orElse(0.0);

            double avgRetrievability = words.stream()
                    .filter(uw -> !"mastered".equals(uw.getStatus()) && !"forgotten".equals(uw.getStatus()))
                    .mapToDouble(uw -> uw.getMemoryRetrievability() != null ? uw.getMemoryRetrievability() : 0.0)
                    .average()
                    .orElse(0.5);

            // Weak: low mastery or declining retrievability
            if (avgMastery < 0.30 || avgRetrievability < 0.50) {
                weakAreas.add(category);
            }

            // Strong: high mastery
            if (avgMastery >= 0.70) {
                strongAreas.add(category);
            }
        }
    }

    // =========================================================================
    // Insight Generation
    // =========================================================================

    /**
     * Generates personalized insights using LLM (in Chinese).
     * Falls back to template-based insights when LLM is not available.
     */
    private String generateInsights(User user, double stability,
                                     Map<String, Object> masteryDistribution,
                                     int optimalDaily, List<String> weakAreas,
                                     List<String> strongAreas,
                                     List<ReviewRecord> reviews) {

        // Build a data summary for the LLM prompt
        int totalReviews = reviews.size();
        long recentCorrect = reviews.stream()
                .skip(Math.max(0, totalReviews - 50))
                .filter(r -> Boolean.TRUE.equals(r.getCorrect()))
                .count();
        int recentTotal = Math.min(totalReviews, 50);
        double recentAccuracy = recentTotal > 0 ? (double) recentCorrect / recentTotal : 0.0;

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) masteryDistribution.getOrDefault("summary", Map.of());

        String dataSummary = String.format(
            "用户: %s\n" +
            "记忆强度(S): %.2f\n" +
            "半衰期: %.1f 天\n" +
            "总学习词数: %s\n" +
            "已掌握词数: %s\n" +
            "平均掌握度: %.2f\n" +
            "平均可提取性: %.2f\n" +
            "近期复习正确率: %.1f%% (%d/%d)\n" +
            "建议每日新词数: %d\n" +
            "薄弱环节: %s\n" +
            "优势领域: %s",
            user.getUsername(),
            stability,
            Math.log(2) * stability / BASE_DECAY_CONSTANT_LAMBDA,
            summary.getOrDefault("totalWords", 0),
            summary.getOrDefault("masteredCount", 0),
            ((Number) summary.getOrDefault("avgMastery", 0.0)).doubleValue(),
            ((Number) summary.getOrDefault("avgRetrievability", 0.0)).doubleValue(),
            recentAccuracy * 100, recentCorrect, recentTotal,
            optimalDaily,
            weakAreas.isEmpty() ? "无明显薄弱环节" : String.join(", ", weakAreas),
            strongAreas.isEmpty() ? "暂无突出优势" : String.join(", ", strongAreas)
        );

        String systemPrompt =
            "你是一位认知科学和记忆研究专家，专门分析语言学习者的记忆模式并提供个性化学习建议。\n" +
            "请根据以下用户的记忆诊断数据，用中文撰写简洁、专业的分析报告。\n\n" +
            "报告应包含：\n" +
            "1. 记忆特征分析：解释该用户的记忆遗忘曲线特征和记忆稳定性\n" +
            "2. 学习进度评估：评价当前词汇掌握情况和学习效率\n" +
            "3. 薄弱与优势分析：指出需要重点关注的领域和已表现良好的领域\n" +
            "4. 个性化建议：给出3-5条具体的、可执行的学习优化建议\n\n" +
            "请用简洁专业的中文回答，避免过于学术化的术语，确保学习者能够理解。";

        String userPrompt = "请分析以下用户的记忆诊断数据并给出学习建议：\n\n" + dataSummary;

        if (!llmConfig.isLlmConfigured()) {
            log.info("LLM not configured, generating fallback insights");
            return generateFallbackInsights(user, stability, summary, optimalDaily,
                    weakAreas, strongAreas, recentAccuracy);
        }

        String llmResponse = callLlm(systemPrompt, userPrompt, 0.7);
        if (llmResponse == null || llmResponse.isBlank()) {
            log.warn("LLM returned empty response, using fallback insights");
            return generateFallbackInsights(user, stability, summary, optimalDaily,
                    weakAreas, strongAreas, recentAccuracy);
        }

        return llmResponse;
    }

    /**
     * Generates template-based insights when LLM is unavailable.
     */
    private String generateFallbackInsights(User user, double stability,
                                             Map<String, Object> summary,
                                             int optimalDaily,
                                             List<String> weakAreas,
                                             List<String> strongAreas,
                                             double recentAccuracy) {
        StringBuilder sb = new StringBuilder();

        sb.append("## 记忆特征分析\n\n");
        double halfLife = Math.log(2) * stability / BASE_DECAY_CONSTANT_LAMBDA;
        if (stability >= 7.0) {
            sb.append("您的记忆强度较强（S=").append(String.format("%.2f", stability))
              .append("），说明您对已学内容的保持能力良好。");
        } else if (stability >= 3.0) {
            sb.append("您的记忆强度处于中等水平（S=").append(String.format("%.2f", stability))
              .append("），建议通过增加复习频率来巩固记忆。");
        } else {
            sb.append("您的记忆强度偏低（S=").append(String.format("%.2f", stability))
              .append("），需要更频繁地进行复习以防止遗忘。");
        }
        sb.append("半衰期约为").append(String.format("%.1f", halfLife))
          .append("天，即记忆保留率降至50%所需的时间。\n\n");

        sb.append("## 学习进度评估\n\n");
        long totalWords = ((Number) summary.getOrDefault("totalWords", 0)).longValue();
        long masteredCount = ((Number) summary.getOrDefault("masteredCount", 0)).longValue();
        double masteryRate = totalWords > 0 ? (double) masteredCount / totalWords : 0.0;
        sb.append("当前共学习 ").append(totalWords).append(" 个词汇，其中掌握 ")
          .append(masteredCount).append(" 个（掌握率 ")
          .append(String.format("%.1f%%", masteryRate * 100)).append("）。");
        if (recentAccuracy > 0.85) {
            sb.append("近期复习正确率较高（").append(String.format("%.1f%%", recentAccuracy * 100))
              .append("），学习效果良好。");
        } else if (recentAccuracy > 0.60) {
            sb.append("近期复习正确率为").append(String.format("%.1f%%", recentAccuracy * 100))
              .append("，仍有提升空间。");
        } else {
            sb.append("近期复习正确率偏低（").append(String.format("%.1f%%", recentAccuracy * 100))
              .append("），建议回顾基础内容。");
        }
        sb.append("\n\n");

        sb.append("## 薄弱与优势分析\n\n");
        if (!weakAreas.isEmpty()) {
            sb.append("薄弱领域：").append(String.join("、", weakAreas))
              .append("。建议针对这些领域进行专项练习。\n");
        } else {
            sb.append("当前无明显薄弱环节，整体学习较为均衡。\n");
        }
        if (!strongAreas.isEmpty()) {
            sb.append("优势领域：").append(String.join("、", strongAreas))
              .append("。可以保持当前的学习节奏。\n");
        }
        sb.append("\n");

        sb.append("## 个性化建议\n\n");
        sb.append("1. 建议每日学习新词数量调整为 **").append(optimalDaily).append("** 个\n");
        if (stability < 3.0) {
            sb.append("2. 建议缩短复习间隔，增加每日复习量以巩固记忆\n");
            sb.append("3. 尝试使用多种复习方式（如拼写、语境造句）加深记忆\n");
        } else {
            sb.append("2. 当前记忆保持良好，可适当增加每日新词量以提高学习效率\n");
            sb.append("3. 对于已掌握的词汇，可通过阅读和听力进行自然巩固\n");
        }
        sb.append("4. 保持每日学习的连续性，连续学习有助于长期记忆的形成\n");
        sb.append("5. 关注薄弱环节的同时，不要忽视对已掌握内容的定期回顾\n");

        return sb.toString();
    }

    // =========================================================================
    // Recommendation Builder
    // =========================================================================

    /**
     * Build a list of actionable recommendation strings for the diagnosis report.
     */
    private List<String> buildRecommendations(double stability, int optimalDaily,
                                               List<String> weakAreas,
                                               List<UserWord> userWords,
                                               List<ReviewRecord> reviews) {
        List<String> recs = new ArrayList<>();

        // Daily word count recommendation
        recs.add("建议每日学习新词数：" + optimalDaily + " 个");

        // Stability-based recommendations
        if (stability < 2.0) {
            recs.add("记忆稳定性较低，建议增加每日复习次数，缩短复习间隔");
            recs.add("尝试使用联想记忆法或词根词缀法加深单词印象");
        } else if (stability < 5.0) {
            recs.add("记忆稳定性中等，建议保持当前复习节奏，重点关注即将到期的复习任务");
        } else {
            recs.add("记忆稳定性良好，可以适当增加每日新词量，提升学习进度");
        }

        // Overdue words check
        LocalDateTime now = LocalDateTime.now();
        long overdueCount = userWords.stream()
                .filter(uw -> uw.getNextReview() != null && uw.getNextReview().isBefore(now))
                .filter(uw -> !"mastered".equals(uw.getStatus()) && !"forgotten".equals(uw.getStatus()))
                .count();
        if (overdueCount > 10) {
            recs.add("当前有 " + overdueCount + " 个单词逾期未复习，建议优先处理这些词汇");
        } else if (overdueCount > 0) {
            recs.add("有 " + overdueCount + " 个单词即将到期，请安排时间复习");
        }

        // Weak area recommendations
        if (!weakAreas.isEmpty()) {
            recs.add("薄弱领域（" + String.join(", ", weakAreas) + "）需要加强专项练习");
        }

        // Accuracy-based recommendation
        if (!reviews.isEmpty()) {
            int windowSize = Math.min(reviews.size(), 50);
            List<ReviewRecord> recent = reviews.subList(reviews.size() - windowSize, reviews.size());
            long correctCount = recent.stream().filter(r -> Boolean.TRUE.equals(r.getCorrect())).count();
            double accuracy = (double) correctCount / recent.size();

            if (accuracy < 0.50) {
                recs.add("近期复习正确率偏低，建议降低每日新词量，集中精力巩固已学内容");
            } else if (accuracy > 0.90) {
                recs.add("近期复习正确率优秀，可以考虑提升学习难度或增加每日新词量");
            }
        }

        return recs;
    }

    // =========================================================================
    // Empty Diagnosis Builder
    // =========================================================================

    /**
     * Build a default/empty diagnosis report for users with no learning data.
     */
    private DiagnosisReport buildEmptyDiagnosis(User user) {
        DiagnosisReport report = new DiagnosisReport();
        report.setMemoryStrength(user.getMemoryStrength() != null ? user.getMemoryStrength() : 1.0);
        report.setForgettingRate(1.0 / DEFAULT_STABILITY);
        report.setHalfLifeDays(DEFAULT_STABILITY * Math.log(2));
        report.setOptimalDailyWords(user.getDailyNewWords() != null ? user.getDailyNewWords() : 20);
        report.setMasteryDistribution(Map.of(
            "statusDistribution", Map.of(),
            "masteryBands", Map.of(
                "0-20%", 0L, "20-40%", 0L, "40-60%", 0L, "60-80%", 0L, "80-100%", 0L
            ),
            "summary", Map.of(
                "totalWords", 0L, "masteredCount", 0L, "learningCount", 0L,
                "reviewCount", 0L, "avgMastery", 0.0, "avgRetrievability", 0.0, "masteryRate", 0.0
            )
        ));
        report.setWeakCategories(Collections.emptyList());
        report.setStrongCategories(Collections.emptyList());
        report.setRecommendations(List.of(
            "尚未开始学习，建议从每日 " + (user.getDailyNewWords() != null ? user.getDailyNewWords() : 20) + " 个新词开始",
            "坚持每日学习和复习，建立稳定的学习习惯",
            "初期建议以认识和理解词义为主，逐步过渡到拼写和运用"
        ));
        report.setCurvePoints(buildDefaultCurvePoints());
        report.setInsightsText(
            "## 记忆诊断报告\n\n" +
            "您尚未开始词汇学习，暂无足够的记忆数据进行分析。\n\n" +
            "### 建议\n" +
            "1. 开始学习新词汇后，系统将自动跟踪您的记忆模式\n" +
            "2. 建议每天坚持学习，保持连续性\n" +
            "3. 系统会根据您的复习表现，自动生成个性化记忆曲线\n" +
            "4. 完成至少5次复习后，即可获得精准的遗忘曲线分析"
        );
        
        // Initialize enhanced fields with empty/neutral values (no fixed data, just placeholders)
        Map<String, Object> learningPatterns = new LinkedHashMap<>();
        learningPatterns.put("status", "insufficient_data");
        learningPatterns.put("message", "需要更多学习数据来分析学习模式");
        learningPatterns.put("preferredDay", "暂无数据");
        learningPatterns.put("preferredTimeSlot", "暂无数据");
        learningPatterns.put("averageSessionDuration", "暂无数据");
        learningPatterns.put("weeklyPattern", Map.of());
        learningPatterns.put("hourlyPattern", Map.of());
        report.setLearningPatterns(learningPatterns);
        
        Map<String, Object> memoryStyle = new LinkedHashMap<>();
        memoryStyle.put("style", "MIXED");
        memoryStyle.put("confidence", 0.3);
        memoryStyle.put("reason", "数据不足，默认混合学习类型");
        memoryStyle.put("characteristics", List.of("需要更多学习数据来确定学习类型"));
        report.setMemoryStyle(memoryStyle);
        
        Map<String, Object> predictiveAnalysis = new LinkedHashMap<>();
        predictiveAnalysis.put("forgettingPrediction", List.of());
        
        Map<String, Object> keyRetentions = new LinkedHashMap<>();
        keyRetentions.put("oneDayRetention", Math.round(Math.exp(-1.0 / DEFAULT_STABILITY) * 10000.0) / 10000.0);
        keyRetentions.put("threeDayRetention", Math.round(Math.exp(-3.0 / DEFAULT_STABILITY) * 10000.0) / 10000.0);
        keyRetentions.put("sevenDayRetention", Math.round(Math.exp(-7.0 / DEFAULT_STABILITY) * 10000.0) / 10000.0);
        keyRetentions.put("fourteenDayRetention", Math.round(Math.exp(-14.0 / DEFAULT_STABILITY) * 10000.0) / 10000.0);
        keyRetentions.put("thirtyDayRetention", Math.round(Math.exp(-30.0 / DEFAULT_STABILITY) * 10000.0) / 10000.0);
        predictiveAnalysis.put("keyRetentions", keyRetentions);
        
        Map<String, Object> highRiskWords = new LinkedHashMap<>();
        highRiskWords.put("highRiskCount", 0L);
        highRiskWords.put("mediumRiskCount", 0L);
        highRiskWords.put("totalAtRisk", 0L);
        highRiskWords.put("highRiskWords", List.of());
        highRiskWords.put("mediumRiskWords", List.of());
        predictiveAnalysis.put("highRiskWords", highRiskWords);
        
        Map<String, Object> masteryTimeEstimation = new LinkedHashMap<>();
        masteryTimeEstimation.put("currentMasteredCount", 0L);
        masteryTimeEstimation.put("currentNonMasteredCount", 0L);
        masteryTimeEstimation.put("currentLearningRate", 0.0);
        masteryTimeEstimation.put("estimatedDaysToMasterCurrent", 0L);
        masteryTimeEstimation.put("targetVocabulary", user.getTargetVocabSize() != null ? user.getTargetVocabSize() : 6000);
        masteryTimeEstimation.put("remainingToTarget", user.getTargetVocabSize() != null ? user.getTargetVocabSize() : 6000);
        masteryTimeEstimation.put("estimatedDaysToTarget", 0L);
        predictiveAnalysis.put("masteryTimeEstimation", masteryTimeEstimation);
        
        predictiveAnalysis.put("reviewNeedByPeriod", Map.of());
        predictiveAnalysis.put("totalNonMasteredWords", 0L);
        predictiveAnalysis.put("totalMasteredWords", 0L);
        predictiveAnalysis.put("estimatedEffort", Map.of());
        
        Map<String, Object> warnings = new LinkedHashMap<>();
        warnings.put("hasWarnings", false);
        warnings.put("warnings", List.of());
        predictiveAnalysis.put("warnings", warnings);
        
        predictiveAnalysis.put("expectedRetention", Math.round(Math.exp(-7.0 / DEFAULT_STABILITY) * 10000.0) / 10000.0);
        predictiveAnalysis.put("riskWordsCount", 0L);
        predictiveAnalysis.put("estimatedMasteryDays", 0L);
        predictiveAnalysis.put("suggestion", "尚未开始学习，暂无预测数据。开始学习后系统将根据您的实际表现生成个性化预测。");
        report.setPredictiveAnalysis(predictiveAnalysis);
        
        return report;
    }

    /**
     * Generate default curve points using the default stability value.
     */
    private List<ForgettingCurvePoint> buildDefaultCurvePoints() {
        List<ForgettingCurvePoint> points = new ArrayList<>();
        for (int day : CURVE_DAYS) {
            ForgettingCurvePoint point = new ForgettingCurvePoint();
            point.setDaysSinceReview(day);
            point.setPredictedRetention(
                Math.round(Math.exp(-day / DEFAULT_STABILITY) * 10000.0) / 10000.0
            );
            point.setActualRetention(null);
            points.add(point);
        }
        return points;
    }
}
