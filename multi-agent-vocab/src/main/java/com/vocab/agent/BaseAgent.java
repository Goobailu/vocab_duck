package com.vocab.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocab.config.LlmConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * Base agent providing LLM API access for all specialized agents.
 */
public abstract class BaseAgent {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected LlmConfig llmConfig;

    @Autowired
    protected ObjectMapper objectMapper;

    private WebClient webClient;

    protected WebClient getWebClient() {
        if (webClient == null) {
            webClient = WebClient.builder()
                    .baseUrl(llmConfig.getApiBase())
                    .defaultHeader("Authorization", "Bearer " + llmConfig.getApiKey())
                    .defaultHeader("Content-Type", "application/json")
                    .codecs(configurer -> configurer
                            .defaultCodecs()
                            .maxInMemorySize(16 * 1024 * 1024))
                    .build();
        }
        return webClient;
    }

    /**
     * Call LLM and return raw text response.
     */
    protected String callLlm(String systemPrompt, String userPrompt, double temperature) {
        if (!llmConfig.isLlmConfigured()) {
            log.warn("LLM API not configured, returning empty response");
            return "";
        }

        try {
            Map<String, Object> body = Map.of(
                "model", llmConfig.getModel(),
                "messages", new Object[]{
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                },
                "temperature", temperature
            );

            String response = getWebClient().post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(llmConfig.getTimeout()))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Connection timed out")) {
                log.warn("LLM API connection timeout, using fallback: {}", e.getMessage());
            } else {
                log.warn("LLM API call failed, using fallback: {}", e.getMessage());
            }
            return "";
        }
    }

    /**
     * Call LLM expecting JSON response.
     */
    protected Map<String, Object> callLlmJson(String systemPrompt, String userPrompt, double temperature) {
        String raw = callLlm(systemPrompt, userPrompt, temperature);
        if (raw.isEmpty()) {
            return Map.of();
        }

        // Try to extract JSON from markdown code blocks
        if (raw.contains("```json")) {
            raw = raw.split("```json")[1].split("```")[0];
        } else if (raw.contains("```")) {
            raw = raw.split("```")[1].split("```")[0];
        }

        try {
            return objectMapper.readValue(raw.trim(), Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse LLM JSON response: {}", e.getMessage());
            return Map.of("raw", raw);
        }
    }

    public abstract String getName();
}
