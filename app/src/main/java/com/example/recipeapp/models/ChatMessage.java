package com.example.recipeapp.models;

public class ChatMessage {
    private String text;
    private boolean fromUser;
    private long timestamp;

    public ChatMessage(String text, boolean fromUser) {
        this.text = text;
        this.fromUser = fromUser;
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() { return text; }
    public boolean isFromUser() { return fromUser; }
    public long getTimestamp() { return timestamp; }
}