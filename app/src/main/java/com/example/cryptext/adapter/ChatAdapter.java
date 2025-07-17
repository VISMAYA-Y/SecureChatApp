package com.example.cryptext.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptext.ChatActivity;
import com.example.cryptext.R;
import com.example.cryptext.model.Chat;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private Context context;
    private List<Chat> chatList;
    private List<Chat> filteredChatList;
    private String currentUserId;
    private Map<String, String> userEmailCache;

    public ChatAdapter(Context context) {
        this.context = context;
        this.chatList = new ArrayList<>();
        this.filteredChatList = new ArrayList<>();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.userEmailCache = new HashMap<>();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = filteredChatList.get(position);

        // Find the other participant (not the current user)
        String otherParticipantId = null;
        for (String participantId : chat.getParticipants()) {
            if (!participantId.equals(currentUserId)) {
                otherParticipantId = participantId;
                break;
            }
        }

        final String recipientId = otherParticipantId;
        
        // Get recipient email
        if (recipientId != null) {
            if (recipientId.startsWith("pending_")) {
                // This is a pending user, extract email from ID
                String placeholderEmail = "user_" + recipientId.substring(8) + "@example.com";
                holder.recipientTextView.setText(placeholderEmail);
                // Cache for future use
                userEmailCache.put(recipientId, placeholderEmail);
            } else if (userEmailCache.containsKey(recipientId)) {
                // Use cached email if available
                holder.recipientTextView.setText(userEmailCache.get(recipientId));
            } else {
                // In a real app, we'd query Firebase for the email
                // For the local version, we'll just use the ID
                String fakeEmail = "user_" + recipientId.substring(0, 5) + "@example.com";
                holder.recipientTextView.setText(fakeEmail);
                // Cache for future use
                userEmailCache.put(recipientId, fakeEmail);
            }
        } else {
            holder.recipientTextView.setText("Unknown User");
        }

        holder.lastMessageTextView.setText(chat.getLastMessage());
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
        String formattedTime = dateFormat.format(new Date(chat.getTimestamp()));
        holder.timeTextView.setText(formattedTime);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Chat clickedChat = filteredChatList.get(holder.getAdapterPosition());
                
                // Find the recipient email
                String clickedRecipientId = null;
                for (String participantId : clickedChat.getParticipants()) {
                    if (!participantId.equals(currentUserId)) {
                        clickedRecipientId = participantId;
                        break;
                    }
                }
                
                // Get the email for this recipient
                String recipientEmail;
                if (clickedRecipientId != null) {
                    if (userEmailCache.containsKey(clickedRecipientId)) {
                        recipientEmail = userEmailCache.get(clickedRecipientId);
                    } else if (clickedRecipientId.startsWith("pending_")) {
                        recipientEmail = "user_" + clickedRecipientId.substring(8) + "@example.com";
                    } else {
                        recipientEmail = "user_" + clickedRecipientId.substring(0, 5) + "@example.com";
                    }
                } else {
                    recipientEmail = "Unknown User";
                }
                
                // Open the chat activity
                openChatActivity(clickedChat.getChatId(), recipientEmail);
            }
        });
    }
    
    private void openChatActivity(String chatId, String recipientEmail) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("recipientEmail", recipientEmail);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return filteredChatList.size();
    }

    public void addChat(Chat chat) {
        if (!chatList.contains(chat)) {
            chatList.add(chat);
            filteredChatList.add(chat);
            notifyDataSetChanged();
        }
    }

    public void filterChats(String query) {
        filteredChatList.clear();
        
        if (query.isEmpty()) {
            filteredChatList.addAll(chatList);
        } else {
            query = query.toLowerCase();
            
            for (Chat chat : chatList) {
                if (chat.getLastMessage().toLowerCase().contains(query)) {
                    filteredChatList.add(chat);
                }
            }
        }
        
        notifyDataSetChanged();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView recipientTextView;
        TextView lastMessageTextView;
        TextView timeTextView;

        ChatViewHolder(View itemView) {
            super(itemView);
            recipientTextView = itemView.findViewById(R.id.recipientEmailTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            timeTextView = itemView.findViewById(R.id.timestampTextView);
        }
    }
} 