package com.example.cryptext;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptext.adapter.ChatAdapter;
import com.example.cryptext.model.Chat;
import com.example.cryptext.util.FirebaseAuthManager;
import com.example.cryptext.util.LocalStorageManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText searchEditText;
    private Button searchButton;
    private ImageButton logoutButton;
    private FloatingActionButton newChatButton;
    private RecyclerView chatsRecyclerView;
    private TextView noChatTextView;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuthManager authManager;
    private LocalStorageManager storageManager;
    private ChatAdapter chatAdapter;
    private FirebaseUser currentUser;
    private HashMap<String, String> userEmailCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase and storage
        firebaseAuth = FirebaseAuth.getInstance();
        authManager = new FirebaseAuthManager(this);
        storageManager = new LocalStorageManager(this);
        currentUser = firebaseAuth.getCurrentUser();

        // Check if user is logged in
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize views
        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        logoutButton = findViewById(R.id.logoutButton);
        newChatButton = findViewById(R.id.newChatButton);
        chatsRecyclerView = findViewById(R.id.chatsRecyclerView);
        noChatTextView = findViewById(R.id.noChatTextView);

        // Set up RecyclerView
        chatAdapter = new ChatAdapter(this);
        chatsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatsRecyclerView.setAdapter(chatAdapter);

        // Initialize cache
        userEmailCache = new HashMap<>();

        // Load user chats
        loadChats();

        // Set up click listeners
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = searchEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(query)) {
                    chatAdapter.filterChats(query);
                }
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });

        newChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewChatDialog();
            }
        });
    }

    private void loadChats() {
        String currentUserId = currentUser.getUid();
        
        // Get chats from local storage
        List<Chat> chats = storageManager.getUserChats(currentUserId);
        
        if (chats.isEmpty()) {
            noChatTextView.setVisibility(View.VISIBLE);
            chatsRecyclerView.setVisibility(View.GONE);
            return;
        }
        
        noChatTextView.setVisibility(View.GONE);
        chatsRecyclerView.setVisibility(View.VISIBLE);
        
        for (Chat chat : chats) {
            chatAdapter.addChat(chat);
        }
    }

    private void showNewChatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Chat");
        
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_new_chat, null);
        final EditText emailEditText = view.findViewById(R.id.emailEditText);
        emailEditText.setTextColor(getResources().getColor(R.color.dark_purple));
        emailEditText.setHintTextColor(getResources().getColor(R.color.hint_dark_purple));
        
        builder.setView(view);
        builder.setPositiveButton("Start Chat", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String recipientEmail = emailEditText.getText().toString().trim();
                
                if (!TextUtils.isEmpty(recipientEmail)) {
                    // Check if email exists in Firebase
                    checkIfUserExists(recipientEmail);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter an email", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        
        builder.create().show();
    }
    
    private void checkIfUserExists(final String recipientEmail) {
        // Show loading dialog but with a timeout
        final AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setTitle("Checking User")
                .setMessage("Searching for user account...")
                .setCancelable(false)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        loadingDialog.show();
        
        // Use our cache if available
        if (userEmailCache.containsKey(recipientEmail)) {
            loadingDialog.dismiss();
            String cachedUserId = userEmailCache.get(recipientEmail);
            if (cachedUserId != null && !cachedUserId.isEmpty() && !cachedUserId.startsWith("pending_")) {
                createNewChat(cachedUserId, recipientEmail);
            } else {
                promptForNonExistentUser(recipientEmail);
            }
            return;
        }
        
        // Set timeout - close dialog after 10 seconds and show options
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Search Timeout")
                        .setMessage("The search is taking longer than expected. What would you like to do?")
                        .setPositiveButton("Continue Waiting", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Continue waiting - show dialog again
                                loadingDialog.show();
                            }
                        })
                        .setNeutralButton("Proceed Anyway", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Create placeholder user ID and proceed
                                String placeholderId = "pending_" + recipientEmail.hashCode();
                                userEmailCache.put(recipientEmail, placeholderId);
                                createNewChat(placeholderId, recipientEmail);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                }
            }
        }, 10000); // 10 second timeout
        
        // Query Firebase for users with this email
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users")
            .whereEqualTo("email", recipientEmail)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                // Dismiss the loading dialog if it's still showing
                if (loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                
                if (!queryDocumentSnapshots.isEmpty()) {
                    // User exists, get the user ID
                    String recipientId = queryDocumentSnapshots.getDocuments().get(0).getId();
                    
                    // Cache the result
                    userEmailCache.put(recipientEmail, recipientId);
                    
                    // Create chat with this user
                    if (!TextUtils.isEmpty(recipientId)) {
                        createNewChat(recipientId, recipientEmail);
                    }
                } else {
                    // User doesn't exist - prompt for options
                    promptForNonExistentUser(recipientEmail);
                }
            })
            .addOnFailureListener(e -> {
                // Dismiss the loading dialog if it's still showing
                if (loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                
                Toast.makeText(MainActivity.this, 
                    "Error checking user: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
                
                promptForNonExistentUser(recipientEmail);
            });
    }
    
    private void promptForNonExistentUser(final String recipientEmail) {
        new AlertDialog.Builder(this)
            .setTitle("User Not Found")
            .setMessage("No account was found with email: " + recipientEmail + "\n\nDo you still want to create a chat? The messages will be delivered when they register.")
            .setPositiveButton("Yes, Create Chat", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Create placeholder user ID
                    String placeholderId = "pending_" + recipientEmail.hashCode();
                    
                    // Cache the placeholder
                    userEmailCache.put(recipientEmail, placeholderId);
                    
                    // Create chat
                    createNewChat(placeholderId, recipientEmail);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void createNewChat(String recipientId, String recipientEmail) {
        String currentUserId = currentUser.getUid();
        
        // Don't create chat with yourself
        if (currentUserId.equals(recipientId)) {
            Toast.makeText(this, "You cannot create a chat with yourself", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if chat already exists with this user in local storage
        List<Chat> userChats = storageManager.getUserChats(currentUserId);
        
        for (Chat chat : userChats) {
            List<String> participants = chat.getParticipants();
            if (participants != null && participants.contains(recipientId)) {
                // Chat already exists, open it
                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                intent.putExtra("chatId", chat.getChatId());
                intent.putExtra("recipientEmail", recipientEmail);
                startActivity(intent);
                return;
            }
        }
        
        // If no existing chat was found, create a new chat
        Chat newChat = storageManager.createChat(currentUserId, recipientId, recipientEmail);
        
        // Open the chat
        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
        intent.putExtra("chatId", newChat.getChatId());
        intent.putExtra("recipientEmail", recipientEmail);
        startActivity(intent);
    }
}