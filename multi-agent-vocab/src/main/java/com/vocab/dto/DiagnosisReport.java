package com.vocab.dto;

import java.util.List;
import java.util.Map;

public class DiagnosisReport {
    private Double memoryStrength;
    private Double forgettingRate;
    private Double halfLifeDays;
    private Integer optimalDailyWords;
    private Map<String, Object> masteryDistribution;
    private List<String> weakCategories;
    private List<String> strongCategories;
    private List<String> recommendations;
    private List<ForgettingCurvePoint> curvePoints;
    private String insightsText;
    
    // Enhanced fields
    private Map<String, Object> learningPatterns;
    private Map<String, Object> memoryStyle;
    private Map<String, Object> predictiveAnalysis;

    public DiagnosisReport() {}

    public Double getMemoryStrength() { return memoryStrength; }
    public void setMemoryStrength(Double memoryStrength) { this.memoryStrength = memoryStrength; }
    public Double getForgettingRate() { return forgettingRate; }
    public void setForgettingRate(Double forgettingRate) { this.forgettingRate = forgettingRate; }
    public Double getHalfLifeDays() { return halfLifeDays; }
    public void setHalfLifeDays(Double halfLifeDays) { this.halfLifeDays = halfLifeDays; }
    public Integer getOptimalDailyWords() { return optimalDailyWords; }
    public void setOptimalDailyWords(Integer optimalDailyWords) { this.optimalDailyWords = optimalDailyWords; }
    public Map<String, Object> getMasteryDistribution() { return masteryDistribution; }
    public void setMasteryDistribution(Map<String, Object> masteryDistribution) { this.masteryDistribution = masteryDistribution; }
    public List<String> getWeakCategories() { return weakCategories; }
    public void setWeakCategories(List<String> weakCategories) { this.weakCategories = weakCategories; }
    public List<String> getStrongCategories() { return strongCategories; }
    public void setStrongCategories(List<String> strongCategories) { this.strongCategories = strongCategories; }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    public List<ForgettingCurvePoint> getCurvePoints() { return curvePoints; }
    public void setCurvePoints(List<ForgettingCurvePoint> curvePoints) { this.curvePoints = curvePoints; }
    public String getInsightsText() { return insightsText; }
    public void setInsightsText(String insightsText) { this.insightsText = insightsText; }
    
    public Map<String, Object> getLearningPatterns() { return learningPatterns; }
    public void setLearningPatterns(Map<String, Object> learningPatterns) { this.learningPatterns = learningPatterns; }
    public Map<String, Object> getMemoryStyle() { return memoryStyle; }
    public void setMemoryStyle(Map<String, Object> memoryStyle) { this.memoryStyle = memoryStyle; }
    public Map<String, Object> getPredictiveAnalysis() { return predictiveAnalysis; }
    public void setPredictiveAnalysis(Map<String, Object> predictiveAnalysis) { this.predictiveAnalysis = predictiveAnalysis; }
}
