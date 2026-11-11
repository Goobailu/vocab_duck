package com.vocab.dto;

import java.util.List;
import java.util.Map;

public class ChatResponse {
    private String reply;
    private List<String> agentsInvoked;
    private List<Map<String, Object>> cards;
    private Map<String, Object> intent;
    private Map<String, Object> data;

    public ChatResponse() {}

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public List<String> getAgentsInvoked() { return agentsInvoked; }
    public void setAgentsInvoked(List<String> agentsInvoked) { this.agentsInvoked = agentsInvoked; }
    public List<Map<String, Object>> getCards() { return cards; }
    public void setCards(List<Map<String, Object>> cards) { this.cards = cards; }
    public Map<String, Object> getIntent() { return intent; }
    public void setIntent(Map<String, Object> intent) { this.intent = intent; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
