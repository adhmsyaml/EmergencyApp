package com.example.emergencyapp2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText username, contact, address, bloodType, medicalNotes, emergencyContacts;
    private Button logoutButton, editProfileButton, saveProfileButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference userDocRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        username = findViewById(R.id.profileUsername);
        contact = findViewById(R.id.profileContact);
        address = findViewById(R.id.profileAddress);
        bloodType = findViewById(R.id.profileBloodType);
        medicalNotes = findViewById(R.id.profileMedicalNotes);
        emergencyContacts = findViewById(R.id.profileEmergencyContacts);
        logoutButton = findViewById(R.id.logoutButton);
        editProfileButton = findViewById(R.id.editProfileButton);
        saveProfileButton = findViewById(R.id.saveProfileButton);

        // Setup button listeners
        logoutButton.setOnClickListener(v -> logoutUser());
        editProfileButton.setOnClickListener(v -> toggleEditMode(true));
        saveProfileButton.setOnClickListener(v -> saveUserProfile());

        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            userDocRef = db.collection("users").document(userId);

            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    username.setText(documentSnapshot.getString("username"));
                    contact.setText(documentSnapshot.getString("contact"));
                    address.setText(documentSnapshot.getString("address"));
                    bloodType.setText(documentSnapshot.getString("bloodType"));
                    medicalNotes.setText(documentSnapshot.getString("medicalNotes"));
                    emergencyContacts.setText(documentSnapshot.getString("emergencyContacts"));
                } else {
                    Toast.makeText(this, "No profile data found.", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show());
        }
    }

    private void saveUserProfile() {
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("username", username.getText().toString().trim());
        updatedData.put("contact", contact.getText().toString().trim());
        updatedData.put("address", address.getText().toString().trim());
        updatedData.put("bloodType", bloodType.getText().toString().trim());
        updatedData.put("medicalNotes", medicalNotes.getText().toString().trim());
        updatedData.put("emergencyContacts", emergencyContacts.getText().toString().trim());

        // Use .set with SetOptions.merge() instead of .update()
        // This will create the document if it's missing, or update it if it exists.
        userDocRef.set(updatedData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                    toggleEditMode(false); // Exit edit mode after saving
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                    Log.e("FIRESTORE_ERROR", "Error writing document", e); // Added for better debugging
                });
    }

    private void toggleEditMode(boolean enable) {
        // Enable or disable the EditText fields
        username.setEnabled(enable);
        contact.setEnabled(enable);
        address.setEnabled(enable);
        bloodType.setEnabled(enable);
        medicalNotes.setEnabled(enable);
        emergencyContacts.setEnabled(enable);

        // Show/hide the appropriate buttons
        if (enable) {
            saveProfileButton.setVisibility(View.VISIBLE);
            editProfileButton.setVisibility(View.GONE);
            username.requestFocus(); // Focus on the first editable field
        } else {
            saveProfileButton.setVisibility(View.GONE);
            editProfileButton.setVisibility(View.VISIBLE);
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}