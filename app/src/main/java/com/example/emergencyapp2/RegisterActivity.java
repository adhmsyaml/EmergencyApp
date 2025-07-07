package com.example.emergencyapp2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText username, email, password;
    private Button registerButton;
    private TextView goToLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Find views
        username = findViewById(R.id.registerUsername);
        email = findViewById(R.id.registerEmail);
        password = findViewById(R.id.registerPassword);
        registerButton = findViewById(R.id.registerButton);
        goToLogin = findViewById(R.id.goToLogin);

        // Listeners
        registerButton.setOnClickListener(v -> createAccount());
        goToLogin.setOnClickListener(v -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
    }

    private void createAccount() {
        String emailStr = email.getText().toString().trim();
        String passwordStr = password.getText().toString().trim();
        String usernameStr = username.getText().toString().trim();

        // Only username, email, and password are required
        if (TextUtils.isEmpty(usernameStr) || TextUtils.isEmpty(emailStr) || TextUtils.isEmpty(passwordStr)) {
            Toast.makeText(this, "Username, Email and Password are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(emailStr, passwordStr)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Registration successful.", Toast.LENGTH_SHORT).show();
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        // Ensure user is not null before proceeding
                        if (firebaseUser != null) {
                            saveUserData(firebaseUser.getUid());
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserData(String userId) {
        // Create Map to store only the essential data
        Map<String, Object> user = new HashMap<>();
        user.put("username", username.getText().toString().trim());
        user.put("email", email.getText().toString().trim());

        // All other fields (contact, gender, address, etc.) will be added later
        // by the user in the ProfileActivity. Do not save them here.

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    // On success, navigate to the main part of the app
                    startActivity(new Intent(RegisterActivity.this, MapsActivity.class));
                    finish(); // Prevent user from going back to registration
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Error saving user data.", Toast.LENGTH_SHORT).show();
                });
    }
}