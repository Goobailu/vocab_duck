package com.vocab.dto;

public class ForgettingCurvePoint {
    private Integer daysSinceReview;
    private Double predictedRetention;
    private Double actualRetention;
    private Double memoryStrength;
    private Double forgettingRate;

    public ForgettingCurvePoint() {}

    public Integer getDaysSinceReview() { return daysSinceReview; }
    public void setDaysSinceReview(Integer daysSinceReview) { this.daysSinceReview = daysSinceReview; }
    public Double getPredictedRetention() { return predictedRetention; }
    public void setPredictedRetention(Double predictedRetention) { this.predictedRetention = predictedRetention; }
    public Double getActualRetention() { return actualRetention; }
    public void setActualRetention(Double actualRetention) { this.actualRetention = actualRetention; }
    public Double getMemoryStrength() { return memoryStrength; }
    public void setMemoryStrength(Double memoryStrength) { this.memoryStrength = memoryStrength; }
    public Double getForgettingRate() { return forgettingRate; }
    public void setForgettingRate(Double forgettingRate) { this.forgettingRate = forgettingRate; }
}
