package com.vocab.controller;

import com.vocab.agent.CoordinatorAgent;
import com.vocab.entity.ChatMessage;
import com.vocab.entity.User;
import com.vocab.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final CoordinatorAgent coordinator;
    private final ChatMessageRepository chatMessageRepository;

    public ChatController(CoordinatorAgent coordinator, ChatMessageRepository chatMessageRepository) {
        this.coordinator = coordinator;
        this.chatMessageRepository = chatMessageRepository;
    }

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String message = body.get("message");

        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "消息不能为空"));
        }

        log.info("Processing chat message from user={}: {}", user.getUsername(), message);

        try {
            ChatMessage userMsg = ChatMessage.builder()
                    .userId(user.getId())
                    .role("user")
                    .content(message)
                    .build();
            chatMessageRepository.save(userMsg);

            long startTime = System.currentTimeMillis();
            Map<String, Object> result = coordinator.handleMessage(user, message);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Message processing completed in {}ms for user={}", duration, user.getUsername());

            String reply = (String) result.getOrDefault("reply", "");
            if (reply == null || reply.isBlank()) {
                result.put("reply", "抱歉，我暂时无法处理你的请求，请稍后再试。");
                reply = (String) result.get("reply");
            }

            @SuppressWarnings("unchecked")
            List<String> agents = (List<String>) result.getOrDefault("agentsInvoked", List.of());
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cards = (List<Map<String, Object>>) result.getOrDefault("cards", List.of());
            log.info("ChatController: reply = {}, cards size = {}, cards = {}", reply, cards.size(), cards);

            ChatMessage assistantMsg = ChatMessage.builder()
                    .userId(user.getId())
                    .role("assistant")
                    .content(reply)
                    .agentType(result.getOrDefault("intent", Map.of()).toString())
                    .metadataJson(serializeMetadata(result))
                    .build();
            chatMessageRepository.save(assistantMsg);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error processing chat message for user={}: {}", user.getUsername(), e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "reply", "抱歉，处理你的请求时出现了问题：" + e.getMessage(),
                    "agents", List.of("coordinator"),
                    "cards", List.of(),
                    "intent", Map.of("intent", "error", "confidence", 0.0)
            ));
        }
    }

    @PostMapping("/end")
    public ResponseEntity<?> endConversation(@AuthenticationPrincipal User user) {
        ChatMessage endMsg = ChatMessage.builder()
                .userId(user.getId())
                .role("system")
                .content("conversation_end")
                .build();
        chatMessageRepository.save(endMsg);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "2") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        List<ChatMessage> allMessages = chatMessageRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        List<List<ChatMessage>> conversations = groupIntoConversations(allMessages);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < Math.min(conversations.size(), limit); i++) {
            List<ChatMessage> conv = conversations.get(i);
            Map<String, Object> convMap = new LinkedHashMap<>();
            convMap.put("time", conv.isEmpty() ? null : (conv.get(0).getCreatedAt() != null ? conv.get(0).getCreatedAt().toString() : null));
            convMap.put("messages", conv.stream().map(m -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", m.getId());
                map.put("role", m.getRole());
                map.put("content", m.getContent());
                map.put("agentType", m.getAgentType());
                map.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().toString() : null);
                
                if (m.getMetadataJson() != null && !m.getMetadataJson().isEmpty()) {
                    try {
                        Map<String, Object> metadata = new com.fasterxml.jackson.databind.ObjectMapper().readValue(m.getMetadataJson(), Map.class);
                        map.put("agents", metadata.getOrDefault("agentsInvoked", List.of()));
                        map.put("cards", metadata.getOrDefault("cards", List.of()));
                        map.put("data", metadata.get("data"));
                    } catch (Exception e) {
                        map.put("agents", List.of());
                        map.put("cards", List.of());
                    }
                } else {
                    map.put("agents", List.of());
                    map.put("cards", List.of());
                }
                
                return map;
            }).collect(Collectors.toList()));
            result.add(convMap);
        }

        return ResponseEntity.ok(result);
    }

    private List<List<ChatMessage>> groupIntoConversations(List<ChatMessage> messages) {
        List<List<ChatMessage>> conversations = new ArrayList<>();
        if (messages.isEmpty()) {
            return conversations;
        }

        List<ChatMessage> currentConv = new ArrayList<>();

        for (ChatMessage msg : messages) {
            if ("system".equals(msg.getRole()) && "conversation_end".equals(msg.getContent())) {
                if (!currentConv.isEmpty()) {
                    Collections.reverse(currentConv);
                    conversations.add(currentConv);
                }
                currentConv = new ArrayList<>();
            } else {
                currentConv.add(msg);
            }
        }

        if (!currentConv.isEmpty()) {
            Collections.reverse(currentConv);
            conversations.add(currentConv);
        }

        return conversations;
    }

    private String serializeMetadata(Map<String, Object> result) {
        try {
            Map<String, Object> meta = new HashMap<>();
            meta.put("agentsInvoked", result.get("agentsInvoked"));
            meta.put("data", result.get("data"));
            meta.put("cards", result.get("cards"));
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(meta);
        } catch (Exception e) {
            return "{}";
        }
    }
}
