package com.vocab.dto;

public class DashboardStats {
    private Long totalWordsLearned;
    private Long wordsMastered;
    private Long wordsDueReview;
    private Integer streakDays;
    private Integer todayNewWords;
    private Integer todayReviews;
    private Double avgRecallRate;
    private Double memoryStrength;
    private String targetExam;
    private Integer dailyNewWordsGoal;

    public DashboardStats() {}

    public Long getTotalWordsLearned() { return totalWordsLearned; }
    public void setTotalWordsLearned(Long totalWordsLearned) { this.totalWordsLearned = totalWordsLearned; }
    public Long getWordsMastered() { return wordsMastered; }
    public void setWordsMastered(Long wordsMastered) { this.wordsMastered = wordsMastered; }
    public Long getWordsDueReview() { return wordsDueReview; }
    public void setWordsDueReview(Long wordsDueReview) { this.wordsDueReview = wordsDueReview; }
    public Integer getStreakDays() { return streakDays; }
    public void setStreakDays(Integer streakDays) { this.streakDays = streakDays; }
    public Integer getTodayNewWords() { return todayNewWords; }
    public void setTodayNewWords(Integer todayNewWords) { this.todayNewWords = todayNewWords; }
    public Integer getTodayReviews() { return todayReviews; }
    public void setTodayReviews(Integer todayReviews) { this.todayReviews = todayReviews; }
    public Double getAvgRecallRate() { return avgRecallRate; }
    public void setAvgRecallRate(Double avgRecallRate) { this.avgRecallRate = avgRecallRate; }
    public Double getMemoryStrength() { return memoryStrength; }
    public void setMemoryStrength(Double memoryStrength) { this.memoryStrength = memoryStrength; }
    public String getTargetExam() { return targetExam; }
    public void setTargetExam(String targetExam) { this.targetExam = targetExam; }
    public Integer getDailyNewWordsGoal() { return dailyNewWordsGoal; }
    public void setDailyNewWordsGoal(Integer dailyNewWordsGoal) { this.dailyNewWordsGoal = dailyNewWordsGoal; }
}
