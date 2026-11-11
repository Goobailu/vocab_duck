package com.vocab.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.URI;

@RestController
@RequestMapping("/api/image")
@CrossOrigin
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final RestTemplate restTemplate;

    @Value("${image.api-key:}")
    private String apiKey;

    @Value("${image.api-base:https://image.pollinations.ai}")
    private String apiBase;

    @Value("${image.model:flux}")
    private String defaultModel;

    public ImageController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/generate")
    public ResponseEntity<byte[]> generateImage(
            @RequestParam String prompt,
            @RequestParam(defaultValue = "1024") int width,
            @RequestParam(defaultValue = "1024") int height,
            @RequestParam(defaultValue = "flux") String model) {
        
        try {
            String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
            
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(apiBase).append("/prompt/").append(encodedPrompt);
            urlBuilder.append("?model=").append(model);
            urlBuilder.append("&width=").append(width);
            urlBuilder.append("&height=").append(height);
            urlBuilder.append("&nologo=true");
            
            if (apiKey != null && !apiKey.isBlank()) {
                urlBuilder.append("&key=").append(apiKey);
            }

            String pollinationsUrl = urlBuilder.toString();
            
            log.info("Generating image for prompt: {}", prompt.substring(0, Math.min(50, prompt.length())) + "...");
            log.debug("Pollinations URL: {}", pollinationsUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "image/*");
            if (apiKey != null && !apiKey.isBlank()) {
                headers.set("Authorization", "Bearer " + apiKey);
            }

            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    URI.create(pollinationsUrl),
                    HttpMethod.GET,
                    request,
                    byte[].class
            );

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.putAll(response.getHeaders());
            responseHeaders.setContentType(MediaType.IMAGE_JPEG);
            
            log.info("Image generation successful, size: {} bytes", response.getBody() != null ? response.getBody().length : 0);
            
            return new ResponseEntity<>(response.getBody(), responseHeaders, response.getStatusCode());
            
        } catch (Exception e) {
            log.error("Image generation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}