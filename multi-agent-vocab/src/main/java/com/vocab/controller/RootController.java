package com.vocab.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
            "name", "Multi-Agent Vocabulary Learning System",
            "version", "1.0.0",
            "description", "基于大语言模型的多智能体背单词系统",
            "agents", List.of(
                "记忆诊断智能体 (Memory Diagnosis)",
                "内容生成智能体 (Content Generation)",
                "复习调度智能体 (Review Scheduling)",
                "协调智能体 (Coordinator)"
            ),
            "techStack", Map.of(
                "backend", "Java Spring Boot 3.3",
                "frontend", "Vue 3 + Vite + Tailwind CSS",
                "database", "MySQL 8.0"
            )
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
