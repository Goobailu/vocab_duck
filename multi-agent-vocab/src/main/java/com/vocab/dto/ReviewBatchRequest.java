package com.vocab.dto;

import java.util.List;

public class ReviewBatchRequest {
    private Long sessionId;
    private List<ReviewSubmitItem> reviews;

    public ReviewBatchRequest() {}

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public List<ReviewSubmitItem> getReviews() { return reviews; }
    public void setReviews(List<ReviewSubmitItem> reviews) { this.reviews = reviews; }
}
