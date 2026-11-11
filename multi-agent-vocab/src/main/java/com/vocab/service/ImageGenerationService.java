package com.vocab.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for generating images via AI image generation API.
 * Supports Pollinations.ai API.
 */
@Service
public class ImageGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationService.class);

    @Value("${image.api-key:}")
    private String apiKey;

    @Value("${image.api-base:https://image.pollinations.ai}")
    private String apiBase;

    @Value("${image.model:flux}")
    private String defaultModel;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * Generates an image from a prompt using Pollinations.ai API.
     * Returns a direct URL to the Pollinations.ai API.
     * 
     * @param prompt The image prompt text
     * @param size Image size (e.g., "256x256", "512x512", "1024x1024")
     * @param quality Image quality (ignored for Pollinations.ai)
     * @return Map containing image URL and metadata, or error information
     */
    public Map<String, Object> generateImage(String prompt, String size, String quality) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (prompt == null || prompt.isBlank()) {
            result.put("status", "error");
            result.put("message", "Prompt cannot be empty");
            return result;
        }

        try {
            String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
            
            int width = 1024;
            int height = 1024;
            if (size != null && size.matches("\\d+x\\d+")) {
                String[] dimensions = size.split("x");
                width = Integer.parseInt(dimensions[0]);
                height = Integer.parseInt(dimensions[1]);
            }

            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(apiBase).append("/prompt/").append(encodedPrompt);
            urlBuilder.append("?model=").append(defaultModel);
            urlBuilder.append("&width=").append(width);
            urlBuilder.append("&height=").append(height);
            urlBuilder.append("&nologo=true");
            
            if (apiKey != null && !apiKey.isBlank()) {
                urlBuilder.append("&key=").append(apiKey);
            }

            String imageUrl = urlBuilder.toString();

            result.put("status", "success");
            result.put("imageUrl", imageUrl);
            result.put("prompt", prompt);
            result.put("size", size != null ? size : "1024x1024");
            result.put("model", defaultModel);
            
            log.info("Image generation URL created for prompt: {}", prompt.substring(0, Math.min(50, prompt.length())) + "...");
            log.debug("Generated URL: {}", imageUrl);

        } catch (Exception e) {
            log.error("Image generation URL construction failed: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "图像生成失败: " + e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
        }

        return result;
    }

    /**
     * Generates an image with default settings.
     */
    public Map<String, Object> generateImage(String prompt) {
        return generateImage(prompt, "1024x1024", "standard");
    }

    /**
     * Checks if image generation is available.
     * Pollinations.ai API can be used without an API key (anonymous access).
     */
    public boolean isAvailable() {
        return true;
    }

    /**
     * Generates multiple variations of an image using different seeds.
     */
    public Map<String, Object> generateImageVariations(String prompt, int count) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        if (count < 1) count = 1;
        if (count > 4) count = 4;

        if (prompt == null || prompt.isBlank()) {
            result.put("status", "error");
            result.put("message", "Prompt cannot be empty");
            return result;
        }

        try {
            String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
            
            java.util.List<String> imageUrls = new java.util.ArrayList<>();
            
            for (int i = 0; i < count; i++) {
                StringBuilder urlBuilder = new StringBuilder();
                urlBuilder.append(apiBase).append("/prompt/").append(encodedPrompt);
                urlBuilder.append("?model=").append(defaultModel);
                urlBuilder.append("&width=1024&height=1024");
                urlBuilder.append("&nologo=true");
                urlBuilder.append("&seed=").append(i * 1000 + 123);
                
                if (apiKey != null && !apiKey.isBlank()) {
                    urlBuilder.append("&key=").append(apiKey);
                }
                
                imageUrls.add(urlBuilder.toString());
            }
            
            result.put("status", "success");
            result.put("imageUrls", imageUrls);
            result.put("count", imageUrls.size());
            
            log.info("Generated {} image variation URLs for prompt: {}", count, 
                    prompt.substring(0, Math.min(50, prompt.length())) + "...");

        } catch (Exception e) {
            log.error("Image variation generation failed: {}", e.getMessage());
            result.put("status", "error");
            result.put("message", "图像生成失败: " + e.getMessage());
        }

        return result;
    }
}
