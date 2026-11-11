package com.vocab.dto;

import jakarta.validation.constraints.*;

public class RegisterRequest {
    @NotBlank @Size(min = 2, max = 50)
    private String username;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6)
    private String password;

    private String targetExam = "cet6";
    private Integer dailyNewWords = 20;

    public RegisterRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getTargetExam() { return targetExam; }
    public void setTargetExam(String targetExam) { this.targetExam = targetExam; }
    public Integer getDailyNewWords() { return dailyNewWords; }
    public void setDailyNewWords(Integer dailyNewWords) { this.dailyNewWords = dailyNewWords; }
}
