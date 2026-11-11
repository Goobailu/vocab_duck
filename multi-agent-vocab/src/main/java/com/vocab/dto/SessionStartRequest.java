package com.vocab.dto;

public class SessionStartRequest {
    private String sessionType = "new_words";

    public SessionStartRequest() {}

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }
}
