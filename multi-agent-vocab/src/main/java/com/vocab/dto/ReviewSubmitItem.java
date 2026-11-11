package com.vocab.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class ReviewSubmitItem {
    private Long wordId;
    private Long userWordId;
    @Min(0) @Max(5)
    private Integer quality;
    private Double responseTime;
    private String reviewType = "recognition";

    public ReviewSubmitItem() {}

    public Long getWordId() { return wordId; }
    public void setWordId(Long wordId) { this.wordId = wordId; }
    public Long getUserWordId() { return userWordId; }
    public void setUserWordId(Long userWordId) { this.userWordId = userWordId; }
    public Integer getQuality() { return quality; }
    public void setQuality(Integer quality) { this.quality = quality; }
    public Double getResponseTime() { return responseTime; }
    public void setResponseTime(Double responseTime) { this.responseTime = responseTime; }
    public String getReviewType() { return reviewType; }
    public void setReviewType(String reviewType) { this.reviewType = reviewType; }
}
