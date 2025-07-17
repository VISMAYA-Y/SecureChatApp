package com.example.cryptext;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.cryptext.adapter.MessageAdapter;
import com.example.cryptext.model.Message;
import com.example.cryptext.util.EncryptionUtil;
import com.example.cryptext.util.LocalStorageManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private TextView recipientEmailTextView;
    private RecyclerView messagesRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private SwipeRefreshLayout swipeRefreshLayout;

    private String chatId;
    private String recipientEmail;
    private MessageAdapter messageAdapter;

    private FirebaseAuth firebaseAuth;
    private LocalStorageManager storageManager;

    // Using chatId as encryption key for simplicity
    // In a real app, you might want a more secure key exchange mechanism
    private String encryptionKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get data from intent
        chatId = getIntent().getStringExtra("chatId");
        recipientEmail = getIntent().getStringExtra("recipientEmail");

        if (chatId == null || recipientEmail == null) {
            Toast.makeText(this, "Error loading chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set encryption key (using chatId as the key)
        // For better security, you should use a proper key exchange method
        encryptionKey = chatId;

        // Initialize Firebase Auth and LocalStorageManager
        firebaseAuth = FirebaseAuth.getInstance();
        storageManager = new LocalStorageManager(this);

        // Initialize views
        recipientEmailTextView = findViewById(R.id.recipientEmailTextView);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Set text colors for message input
        messageEditText.setTextColor(getResources().getColor(R.color.dark_purple));
        messageEditText.setHintTextColor(getResources().getColor(R.color.hint_dark_purple));

        // Set recipient email
        recipientEmailTextView.setText(recipientEmail);

        // Set up RecyclerView
        messageAdapter = new MessageAdapter(this, firebaseAuth.getCurrentUser().getUid());
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messageAdapter);

        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Reload messages from local storage
                loadMessages();
            }
        });

        // Load messages
        loadMessages();

        // Set up send button
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(messageText)) {
                    sendMessage(messageText);
                    messageEditText.setText("");
                }
            }
        });
    }

    private void loadMessages() {
        sendButton.setEnabled(false); // Disable button during loading
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        // Get messages from local storage
        List<Message> messageList = storageManager.getChatMessages(chatId);
        List<Message> decryptedMessages = new ArrayList<>();

        for (Message message : messageList) {
            // Decrypt message content if it exists
            if (message != null && message.getContent() != null) {
                String decryptedContent = EncryptionUtil.decrypt(message.getContent(), encryptionKey);
                if (decryptedContent != null) {
                    Message decryptedMessage = new Message(
                            message.getMessageId(),
                            message.getSenderId(),
                            decryptedContent,
                            message.getTimestamp()
                    );
                    decryptedMessages.add(decryptedMessage);
                } else {
                    // If decryption failed, still show the message but mark it
                    Message errorMessage = new Message(
                            message.getMessageId(),
                            message.getSenderId(),
                            "[Encrypted message]",
                            message.getTimestamp()
                    );
                    decryptedMessages.add(errorMessage);
                }
            }
        }

        messageAdapter.setMessages(decryptedMessages);

        // Scroll to bottom
        if (decryptedMessages.size() > 0) {
            messagesRecyclerView.smoothScrollToPosition(decryptedMessages.size() - 1);
        }

        sendButton.setEnabled(true);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void sendMessage(final String content) {
        String senderId = firebaseAuth.getCurrentUser().getUid();

        // Encrypt the message content
        String encryptedContent = EncryptionUtil.encrypt(content, encryptionKey);
        if (encryptedContent == null) {
            Toast.makeText(this, "Error encrypting message", Toast.LENGTH_SHORT).show();
            return;
        }

        sendButton.setEnabled(false); // Disable button during sending

        // Save message to local storage
        boolean success = storageManager.sendMessage(chatId, senderId, content, encryptedContent);

        if (success) {
            // Message sent successfully, reload messages
            loadMessages();
        } else {
            Toast.makeText(ChatActivity.this, "Error saving message", Toast.LENGTH_SHORT).show();
            sendButton.setEnabled(true);
        }
    }
} 