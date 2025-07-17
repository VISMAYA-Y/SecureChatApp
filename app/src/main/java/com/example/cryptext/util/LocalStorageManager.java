package com.example.cryptext.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.cryptext.model.Chat;
import com.example.cryptext.model.Message;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to handle local storage operations for the messaging app.
 * This simulates a server by storing data in SharedPreferences.
 */
public class LocalStorageManager {
    private static final String TAG = "LocalStorageManager";
    
    // SharedPreferences file names
    private static final String PREF_CHATS = "chats_data";
    private static final String PREF_MESSAGES = "messages_data";
    private static final String PREF_USER_CHATS = "user_chats_data";
    
    private final Context context;
    private final Gson gson;
    
    public LocalStorageManager(Context context) {
        this.context = context;
        this.gson = new Gson();
    }
    
    // USER CHATS METHODS
    
    /**
     * Get all chats for a specific user
     */
    public List<Chat> getUserChats(String userId) {
        SharedPreferences userChatsPrefs = context.getSharedPreferences(PREF_USER_CHATS, Context.MODE_PRIVATE);
        String userChatsJson = userChatsPrefs.getString(userId, null);
        
        if (userChatsJson == null) {
            return new ArrayList<>();
        }
        
        Type chatListType = new TypeToken<List<String>>() {}.getType();
        List<String> chatIds = gson.fromJson(userChatsJson, chatListType);
        
        List<Chat> chats = new ArrayList<>();
        SharedPreferences chatsPrefs = context.getSharedPreferences(PREF_CHATS, Context.MODE_PRIVATE);
        
        for (String chatId : chatIds) {
            String chatJson = chatsPrefs.getString(chatId, null);
            if (chatJson != null) {
                Chat chat = gson.fromJson(chatJson, Chat.class);
                chats.add(chat);
            }
        }
        
        return chats;
    }
    
    /**
     * Create a new chat between two users
     */
    public Chat createChat(String currentUserId, String recipientId, String recipientEmail) {
        // Generate a unique chat ID
        String chatId = "chat_" + System.currentTimeMillis() + "_" + currentUserId.substring(0, 4);
        long timestamp = System.currentTimeMillis();
        
        // Create chat object
        List<String> participants = new ArrayList<>();
        participants.add(currentUserId);
        participants.add(recipientId);
        
        Chat chat = new Chat();
        chat.setChatId(chatId);
        chat.setLastMessage("Start chatting");
        chat.setTimestamp(timestamp);
        chat.setParticipants(participants);
        
        // Save the chat
        SharedPreferences chatsPrefs = context.getSharedPreferences(PREF_CHATS, Context.MODE_PRIVATE);
        String chatJson = gson.toJson(chat);
        chatsPrefs.edit().putString(chatId, chatJson).apply();
        
        // Add chat to both users' chat lists
        addChatToUser(currentUserId, chatId);
        addChatToUser(recipientId, chatId);
        
        return chat;
    }
    
    private void addChatToUser(String userId, String chatId) {
        SharedPreferences userChatsPrefs = context.getSharedPreferences(PREF_USER_CHATS, Context.MODE_PRIVATE);
        String userChatsJson = userChatsPrefs.getString(userId, null);
        
        List<String> chatIds;
        if (userChatsJson == null) {
            chatIds = new ArrayList<>();
        } else {
            Type chatListType = new TypeToken<List<String>>() {}.getType();
            chatIds = gson.fromJson(userChatsJson, chatListType);
        }
        
        if (!chatIds.contains(chatId)) {
            chatIds.add(chatId);
            userChatsPrefs.edit().putString(userId, gson.toJson(chatIds)).apply();
        }
    }
    
    // MESSAGES METHODS
    
    /**
     * Get all messages for a specific chat
     */
    public List<Message> getChatMessages(String chatId) {
        SharedPreferences messagesPrefs = context.getSharedPreferences(PREF_MESSAGES, Context.MODE_PRIVATE);
        String chatMessagesJson = messagesPrefs.getString(chatId, null);
        
        if (chatMessagesJson == null) {
            return new ArrayList<>();
        }
        
        Type messageListType = new TypeToken<List<Message>>() {}.getType();
        return gson.fromJson(chatMessagesJson, messageListType);
    }
    
    /**
     * Save a new message to a chat
     */
    public boolean sendMessage(String chatId, String senderId, String content, String encryptedContent) {
        try {
            // Get existing messages
            List<Message> messages = getChatMessages(chatId);
            
            // Create new message
            String messageId = "msg_" + System.currentTimeMillis() + "_" + senderId.substring(0, 4);
            long timestamp = System.currentTimeMillis();
            
            Message message = new Message(messageId, senderId, encryptedContent, timestamp);
            messages.add(message);
            
            // Save updated messages
            SharedPreferences messagesPrefs = context.getSharedPreferences(PREF_MESSAGES, Context.MODE_PRIVATE);
            String messagesJson = gson.toJson(messages);
            messagesPrefs.edit().putString(chatId, messagesJson).apply();
            
            // Update chat's last message
            updateChatLastMessage(chatId, content, timestamp);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending message", e);
            return false;
        }
    }
    
    private void updateChatLastMessage(String chatId, String lastMessage, long timestamp) {
        SharedPreferences chatsPrefs = context.getSharedPreferences(PREF_CHATS, Context.MODE_PRIVATE);
        String chatJson = chatsPrefs.getString(chatId, null);
        
        if (chatJson != null) {
            Chat chat = gson.fromJson(chatJson, Chat.class);
            chat.setLastMessage(lastMessage);
            chat.setTimestamp(timestamp);
            
            chatsPrefs.edit().putString(chatId, gson.toJson(chat)).apply();
        }
    }
    
    /**
     * Clear all local data (for testing or logout)
     */
    public void clearAllData() {
        context.getSharedPreferences(PREF_CHATS, Context.MODE_PRIVATE).edit().clear().apply();
        context.getSharedPreferences(PREF_MESSAGES, Context.MODE_PRIVATE).edit().clear().apply();
        context.getSharedPreferences(PREF_USER_CHATS, Context.MODE_PRIVATE).edit().clear().apply();
    }
} 