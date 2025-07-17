package com.example.cryptext.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to handle Firebase Authentication
 */
public class FirebaseAuthManager {
    private static final String TAG = "FirebaseAuthManager";

    private Context context;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    
    public FirebaseAuthManager(Context context) {
        this.context = context;
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    // AUTH METHODS
    
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception e);
    }
    
    public void signIn(String email, String password, final AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure(task.getException());
                    }
                }
            });
    }
    
    public void register(final String email, String password, final AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            createUserProfile(user, callback);
                        } else {
                            callback.onFailure(new Exception("Failed to create user"));
                        }
                    } else {
                        callback.onFailure(task.getException());
                    }
                }
            });
    }
    
    private void createUserProfile(final FirebaseUser user, final AuthCallback callback) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", user.getUid());
        userData.put("email", user.getEmail());
        userData.put("createdAt", System.currentTimeMillis());
        
        firestore.collection("users")
            .document(user.getUid())
            .set(userData)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    callback.onSuccess(user);
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    callback.onFailure(e);
                }
            });
    }
    
    public void signOut() {
        firebaseAuth.signOut();
    }
    
    // Get the current user
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }
    
    // Check if user is signed in
    public boolean isUserSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }
} 