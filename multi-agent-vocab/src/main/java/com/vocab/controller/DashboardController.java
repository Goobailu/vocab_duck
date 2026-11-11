package com.vocab.controller;

import com.vocab.agent.MemoryDiagnosisAgent;
import com.vocab.agent.ReviewSchedulingAgent;
import com.vocab.dto.DiagnosisReport;
import com.vocab.entity.LearningSession;
import com.vocab.entity.User;
import com.vocab.entity.UserWord;
import com.vocab.entity.Word;
import com.vocab.repository.LearningSessionRepository;
import com.vocab.repository.UserRepository;
import com.vocab.repository.UserWordRepository;
import com.vocab.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final MemoryDiagnosisAgent diagnosisAgent;
    private final ReviewSchedulingAgent schedulingAgent;
    private final UserWordRepository userWordRepository;
    private final LearningSessionRepository sessionRepository;
    private final WordRepository wordRepository;
    private final UserRepository userRepository;

    public DashboardController(MemoryDiagnosisAgent diagnosisAgent, ReviewSchedulingAgent schedulingAgent,
                               UserWordRepository userWordRepository, LearningSessionRepository sessionRepository,
                               WordRepository wordRepository, UserRepository userRepository) {
        this.diagnosisAgent = diagnosisAgent;
        this.schedulingAgent = schedulingAgent;
        this.userWordRepository = userWordRepository;
        this.sessionRepository = sessionRepository;
        this.wordRepository = wordRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@AuthenticationPrincipal User user) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        long totalLearned = userWordRepository.countByUserId(user.getId());
        long mastered = userWordRepository.countByUserIdAndStatus(user.getId(), "mastered");

        // 待复习数量：使用数据库计数查询获取真实总数（包含今天所有待复习的单词）
        List<String> reviewStatuses = List.of("learning", "review", "new");
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        long due = userWordRepository.countByUserIdAndNextReviewBeforeAndStatusIn(user.getId(), todayEnd, reviewStatuses);

        // DEBUG: Log values for troubleshooting
        System.out.println("=== DEBUG Dashboard Stats ===");
        System.out.println("User ID: " + user.getId());
        System.out.println("User Username: " + user.getUsername());
        System.out.println("Total Learned: " + totalLearned);
        System.out.println("Mastered: " + mastered);
        System.out.println("Due Words Count: " + due);

        // 今日新学单词数：基于 UserWord 记录的实际创建时间（比 session 更准确）
        long todayNew = userWordRepository.countTodayLearnedWords(user.getId(), todayStart);
        System.out.println("Today New Words: " + todayNew);

        List<LearningSession> todaySessions = sessionRepository.findByUserIdAndStartedAtBetween(
                user.getId(), todayStart, now);

        int todayReviews = todaySessions.stream()
                .filter(s -> "review".equals(s.getSessionType()))
                .mapToInt(LearningSession::getWordsStudied)
                .sum();

        Double avgMastery = userWordRepository.findAvgMastery(user.getId());

        // Compute streak from combined session + UserWord history (same source as study-history)
        Set<LocalDate> studyDates = buildStudyDates(user);
        int computedStreak = 0;
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 365; i++) {
            LocalDate checkDate = today.minusDays(i);
            if (studyDates.contains(checkDate)) {
                computedStreak++;
            } else if (i > 0) {
                break;
            }
        }
        // Sync computed streak to user entity
        if (user.getStreakDays() == null || user.getStreakDays() != computedStreak) {
            user.setStreakDays(computedStreak);
            userRepository.save(user);
        }

        return ResponseEntity.ok(Map.of(
                "totalWordsLearned", totalLearned,
                "wordsMastered", mastered,
                "wordsDueReview", due,
                "streakDays", computedStreak,
                "todayNewWords", todayNew,
                "todayReviews", todayReviews,
                "avgRecallRate", avgMastery != null ? Math.round(avgMastery * 1000.0) / 1000.0 : 0.0,
                "memoryStrength", Math.round(user.getMemoryStrength() * 1000.0) / 1000.0,
                "targetExam", user.getTargetExam(),
                "dailyNewWordsGoal", user.getDailyNewWords()
        ));
    }

    @GetMapping("/diagnosis")
    public ResponseEntity<?> getDiagnosis(@AuthenticationPrincipal User user) {
        DiagnosisReport report = diagnosisAgent.diagnose(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("memoryStrength", report.getMemoryStrength());
        result.put("forgettingRate", report.getForgettingRate());
        result.put("halfLifeDays", report.getHalfLifeDays());
        result.put("optimalDailyWords", report.getOptimalDailyWords());
        result.put("masteryDistribution", report.getMasteryDistribution());
        result.put("weakCategories", report.getWeakCategories());
        result.put("strongCategories", report.getStrongCategories());
        result.put("recommendations", report.getRecommendations());
        result.put("curvePoints", report.getCurvePoints());
        result.put("insightsText", report.getInsightsText());
        
        // Enhanced fields
        result.put("learningPatterns", report.getLearningPatterns());
        result.put("memoryStyle", report.getMemoryStyle());
        result.put("predictiveAnalysis", report.getPredictiveAnalysis());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/optimal-times")
    public ResponseEntity<?> getOptimalStudyTimes(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(schedulingAgent.getOptimalStudyTimes(user));
    }

    @GetMapping("/difficulty-adjustment")
    public ResponseEntity<?> getDifficultyAdjustment(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(schedulingAgent.adjustDifficultyLevel(user));
    }

    @GetMapping("/daily-plan")
    public ResponseEntity<?> getDailyPlan(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "60") int availableMinutes) {
        return ResponseEntity.ok(schedulingAgent.generateDailyPlan(user, availableMinutes));
    }

    @GetMapping("/urgent-words")
    public ResponseEntity<?> getUrgentWords(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(schedulingAgent.getUrgentWords(user, limit));
    }

    @GetMapping("/schedule")
    public ResponseEntity<?> getSchedule(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(schedulingAgent.getScheduleOverview(user));
    }

    @GetMapping("/progress")
    public ResponseEntity<?> getProgress(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "30") int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<LearningSession> sessions = sessionRepository.findByUserIdSince(user.getId(), since);
        List<UserWord> userWords = userWordRepository.findByUserId(user.getId());

        Map<String, Map<String, Object>> daily = new LinkedHashMap<>();

        for (LearningSession s : sessions) {
            String dayKey = s.getStartedAt().toLocalDate().toString();
            daily.computeIfAbsent(dayKey, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("date", k);
                m.put("newWords", 0);
                m.put("reviews", 0);
                m.put("score", 0.0);
                m.put("sessions", 0);
                m.put("avgSessionScore", 0.0);
                m.put("totalScore", 0.0);
                m.put("scoreCount", 0);
                return m;
            });
            Map<String, Object> d = daily.get(dayKey);
            d.put("sessions", (int) d.get("sessions") + 1);
            if ("review".equals(s.getSessionType())) {
                d.put("reviews", (int) d.get("reviews") + s.getWordsStudied());
            }
            if (s.getSessionScore() != null) {
                d.put("score", Math.max((double) d.get("score"), s.getSessionScore()));
                d.put("totalScore", (double) d.get("totalScore") + s.getSessionScore());
                d.put("scoreCount", (int) d.get("scoreCount") + 1);
            }
        }

        List<Object[]> newWordsByDate = userWordRepository.countNewWordsByDate(user.getId(), since);
        for (Object[] row : newWordsByDate) {
            String dayKey = row[0].toString();
            long count = (Long) row[1];
            daily.computeIfAbsent(dayKey, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("date", k);
                m.put("newWords", 0);
                m.put("reviews", 0);
                m.put("score", 0.0);
                m.put("sessions", 0);
                m.put("avgSessionScore", 0.0);
                m.put("totalScore", 0.0);
                m.put("scoreCount", 0);
                return m;
            });
            daily.get(dayKey).put("newWords", (int) count);
        }

        double totalMemoryStrength = 0.0;
        int wordsWithStrength = 0;
        for (UserWord uw : userWords) {
            if (uw.getMemoryStability() != null) {
                totalMemoryStrength += uw.getMemoryStability();
                wordsWithStrength++;
            }
        }
        double avgMemoryStrength = wordsWithStrength > 0 ? totalMemoryStrength / wordsWithStrength : 1.0;

        for (Map<String, Object> d : daily.values()) {
            int scoreCount = (int) d.get("scoreCount");
            if (scoreCount > 0) {
                d.put("avgSessionScore", Math.round((double) d.get("totalScore") / scoreCount * 1000.0) / 1000.0);
            } else {
                d.put("avgSessionScore", 0.0);
            }
            d.remove("totalScore");
            d.remove("scoreCount");
        }

        return ResponseEntity.ok(Map.of(
                "dailyProgress", new ArrayList<>(daily.values()),
                "totalDays", days,
                "activeDays", daily.size(),
                "avgMemoryStrength", Math.round(avgMemoryStrength * 1000.0) / 1000.0
        ));
    }

    @GetMapping("/mastery-distribution")
    public ResponseEntity<?> getMasteryDistribution(@AuthenticationPrincipal User user) {
        List<UserWord> userWords = userWordRepository.findByUserId(user.getId());

        Map<Integer, Map<String, Object>> byDiff = new LinkedHashMap<>();
        for (UserWord uw : userWords) {
            Word w;
            try { w = uw.getWord(); } catch (Exception e) { continue; }
            if (w == null) continue;
            int diff = w.getDifficulty();
            Map<String, Object> d = byDiff.computeIfAbsent(diff, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("difficulty", k);
                m.put("total", 0);
                m.put("mastered", 0);
                m.put("masterySum", 0.0);
                m.put("strengthSum", 0.0);
                m.put("strengthCount", 0);
                return m;
            });
            d.put("total", (int) d.get("total") + 1);
            d.put("masterySum", (double) d.get("masterySum") + (uw.getMasteryLevel() != null ? uw.getMasteryLevel() : 0.0));
            if (uw.getMemoryStability() != null) {
                d.put("strengthSum", (double) d.get("strengthSum") + uw.getMemoryStability());
                d.put("strengthCount", (int) d.get("strengthCount") + 1);
            }
            if ("mastered".equals(uw.getStatus())) {
                d.put("mastered", (int) d.get("mastered") + 1);
            }
        }

        List<Map<String, Object>> result = byDiff.values().stream().map(d -> {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("difficulty", d.get("difficulty"));
            out.put("total", d.get("total"));
            out.put("mastered", d.get("mastered"));
            int total = (int) d.get("total");
            double sum = (double) d.get("masterySum");
            out.put("avgMastery", total > 0 ? Math.round(sum / total * 1000.0) / 1000.0 : 0.0);
            
            int strengthCount = (int) d.get("strengthCount");
            double strengthSum = (double) d.get("strengthSum");
            out.put("avgMemoryStrength", strengthCount > 0 ? Math.round(strengthSum / strengthCount * 1000.0) / 1000.0 : 1.0);
            
            double avgStrength = strengthCount > 0 ? strengthSum / strengthCount : 1.0;
            double forgettingRate = 0.1 / avgStrength;
            out.put("avgForgettingRate", Math.round(forgettingRate * 10000.0) / 10000.0);
            out.put("avgHalfLife", Math.round(Math.log(2) / forgettingRate * 10.0) / 10.0);
            
            return out;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("distribution", result));
    }
    
    @GetMapping("/study-history")
    public ResponseEntity<?> getStudyHistory(@AuthenticationPrincipal User user, @RequestParam(defaultValue = "90") int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<LearningSession> sessions = sessionRepository.findByUserIdSince(user.getId(), since);
        
        Map<String, Map<String, Object>> dailyStats = new LinkedHashMap<>();
        
        for (LearningSession s : sessions) {
            String dateKey = s.getStartedAt().toLocalDate().toString();
            dailyStats.computeIfAbsent(dateKey, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("date", k);
                m.put("hasStudy", false);
                m.put("newWords", 0);
                m.put("reviews", 0);
                m.put("totalWords", 0);
                return m;
            });
            Map<String, Object> day = dailyStats.get(dateKey);
            day.put("hasStudy", true);
            if ("review".equals(s.getSessionType())) {
                day.put("reviews", (int) day.get("reviews") + (s.getWordsStudied() != null ? s.getWordsStudied() : 0));
            }
        }
        
        List<Object[]> newWordsByDate = userWordRepository.countNewWordsByDate(user.getId(), since);
        for (Object[] row : newWordsByDate) {
            String dateKey = row[0].toString();
            long count = (Long) row[1];
            dailyStats.computeIfAbsent(dateKey, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("date", k);
                m.put("hasStudy", false);
                m.put("newWords", 0);
                m.put("reviews", 0);
                m.put("totalWords", 0);
                return m;
            });
            Map<String, Object> day = dailyStats.get(dateKey);
            day.put("hasStudy", true);
            day.put("newWords", (int) count);
        }
        
        for (Map<String, Object> day : dailyStats.values()) {
            int newWords = (int) day.getOrDefault("newWords", 0);
            int reviews = (int) day.getOrDefault("reviews", 0);
            day.put("totalWords", newWords + reviews);
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateKey = date.toString();
            Map<String, Object> day = dailyStats.getOrDefault(dateKey, Map.of(
                "date", dateKey,
                "hasStudy", false,
                "newWords", 0,
                "reviews", 0,
                "totalWords", 0
            ));
            result.add(day);
        }
        
        return ResponseEntity.ok(Map.of(
            "history", result,
            "totalDays", days,
            "activeDays", dailyStats.size()
        ));
    }

    @GetMapping("/streak-calculation")
    public ResponseEntity<?> calculateStreak(@AuthenticationPrincipal User user) {
        Set<LocalDate> studyDates = buildStudyDates(user);

        int currentStreak = 0;
        LocalDate today = LocalDate.now();
        
        for (int i = 0; i < 365; i++) {
            LocalDate checkDate = today.minusDays(i);
            if (studyDates.contains(checkDate)) {
                currentStreak++;
            } else if (i > 0) {
                break;
            }
        }
        
        int longestStreak = 0;
        int tempStreak = 0;
        LocalDate earliestDate = studyDates.stream().min(LocalDate::compareTo).orElse(today);
        
        for (LocalDate date = earliestDate; !date.isAfter(today); date = date.plusDays(1)) {
            if (studyDates.contains(date)) {
                tempStreak++;
                longestStreak = Math.max(longestStreak, tempStreak);
            } else {
                tempStreak = 0;
            }
        }
        
        int totalActiveDays = studyDates.size();
        
        // Sync computed streak to user.streakDays for consistency
        if (user.getStreakDays() == null || user.getStreakDays() != currentStreak) {
            user.setStreakDays(currentStreak);
            userRepository.save(user);
        }
        
        return ResponseEntity.ok(Map.of(
            "currentStreak", currentStreak,
            "longestStreak", longestStreak,
            "totalActiveDays", totalActiveDays
        ));
    }

    /**
     * Build a unified set of study dates from BOTH LearningSession and UserWord.firstLearned.
     * This ensures streak calculation is consistent with the study-history display,
     * which also uses both data sources.
     */
    private Set<LocalDate> buildStudyDates(User user) {
        Set<LocalDate> studyDates = new HashSet<>();

        // Source 1: LearningSession dates
        List<LearningSession> sessions = sessionRepository.findByUserIdOrderByStartedAtDesc(user.getId());
        for (LearningSession s : sessions) {
            if (s.getStartedAt() != null) {
                studyDates.add(s.getStartedAt().toLocalDate());
            }
        }

        // Source 2: UserWord.firstLearned dates (covers words learned without sessions)
        LocalDateTime since = LocalDateTime.now().minusDays(365);
        List<Object[]> newWordsByDate = userWordRepository.countNewWordsByDate(user.getId(), since);
        for (Object[] row : newWordsByDate) {
            String dateStr = row[0].toString();
            try {
                studyDates.add(LocalDate.parse(dateStr));
            } catch (Exception ignored) {
                // skip unparseable dates
            }
        }

        return studyDates;
    }
}
