package com.vocab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LlmConfig {
    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.api-base:https://api.openai.com/v1}")
    private String apiBase;

    @Value("${llm.model:gpt-4o-mini}")
    private String model;

    @Value("${llm.timeout:60}")
    private int timeout;

    @Value("${image.api-key:}")
    private String imageApiKey;

    @Value("${image.api-base:https://api.openai.com/v1}")
    private String imageApiBase;

    @Value("${sr.initial-ef:2.5}")
    private double initialEf;

    @Value("${sr.min-ef:1.3}")
    private double minEf;

    @Value("${sr.first-interval:1}")
    private int firstInterval;

    @Value("${sr.second-interval:6}")
    private int secondInterval;

    @Value("${learning.daily-new-words-default:20}")
    private int dailyNewWordsDefault;

    @Value("${learning.max-daily-new-words:50}")
    private int maxDailyNewWords;

    public String getApiKey() { return apiKey; }
    public String getApiBase() { return apiBase; }
    public String getModel() { return model; }
    public int getTimeout() { return timeout; }
    public String getImageApiKey() { return imageApiKey; }
    public String getImageApiBase() { return imageApiBase; }
    public double getInitialEf() { return initialEf; }
    public double getMinEf() { return minEf; }
    public int getFirstInterval() { return firstInterval; }
    public int getSecondInterval() { return secondInterval; }
    public int getDailyNewWordsDefault() { return dailyNewWordsDefault; }
    public int getMaxDailyNewWords() { return maxDailyNewWords; }

    public boolean isLlmConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
