package com.example.cryptext.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chat {
    private String chatId;
    private String lastMessage;
    private long timestamp;
    private List<String> participants;

    // Required empty constructor for Firestore
    public Chat() {
        participants = new ArrayList<>();
    }

    public Chat(String chatId, String lastMessage, long timestamp) {
        this.chatId = chatId;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.participants = new ArrayList<>();
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public void addParticipant(String userId) {
        if (participants == null) {
            participants = new ArrayList<>();
        }
        
        if (!participants.contains(userId)) {
            participants.add(userId);
        }
    }
} 