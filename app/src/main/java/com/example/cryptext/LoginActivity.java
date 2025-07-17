package com.example.cryptext;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cryptext.util.FirebaseAuthManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton;
    private SignInButton googleSignInButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuthManager authManager;
    private GoogleSignInClient googleSignInClient;

    private static final int RC_SIGN_IN = 9001;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        authManager = new FirebaseAuthManager(this);

        // Configure Google Sign-in
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);

        // Set text colors
        emailEditText.setTextColor(getResources().getColor(R.color.dark_purple));
        emailEditText.setHintTextColor(getResources().getColor(R.color.hint_dark_purple));
        passwordEditText.setTextColor(getResources().getColor(R.color.dark_purple));
        passwordEditText.setHintTextColor(getResources().getColor(R.color.hint_dark_purple));

        // Set click listeners
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoginMode) {
                    // Switch to registration mode
                    isLoginMode = false;
                    updateUIForMode();
                } else {
                    // Register user
                    registerUser();
                }
            }
        });

        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });
        
        // Set initial UI
        updateUIForMode();
    }
    
    private void updateUIForMode() {
        if (isLoginMode) {
            loginButton.setVisibility(View.VISIBLE);
            registerButton.setText("Register");
            // Other UI adjustments for login mode
        } else {
            loginButton.setVisibility(View.GONE);
            registerButton.setText("Sign Up");
            // Other UI adjustments for register mode
        }
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        loginButton.setEnabled(false);

        authManager.signIn(email, password, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                loginButton.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                loginButton.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }
        
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return;
        }

        registerButton.setEnabled(false);

        authManager.register(email, password, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                registerButton.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                registerButton.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Registration failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        googleSignInButton.setEnabled(false);

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        googleSignInButton.setEnabled(true);
                        
                        if (task.isSuccessful()) {
                            // Sign in success
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            Toast.makeText(LoginActivity.this, "Google sign-in successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            // Sign in failed
                            Toast.makeText(LoginActivity.this, "Google sign-in failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
} 