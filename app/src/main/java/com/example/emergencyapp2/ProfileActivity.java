package com.example.emergencyapp2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText username, contact, address, medicalNotes, emergencyContacts;
    private Spinner bloodTypeSpinner; // Changed from EditText
    private Button logoutButton, editProfileButton, saveProfileButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference userDocRef;
    private ArrayAdapter<String> bloodTypeAdapter;

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
        medicalNotes = findViewById(R.id.profileMedicalNotes);
        emergencyContacts = findViewById(R.id.profileEmergencyContacts);
        bloodTypeSpinner = findViewById(R.id.profileBloodTypeSpinner); // New Spinner
        logoutButton = findViewById(R.id.logoutButton);
        editProfileButton = findViewById(R.id.editProfileButton);
        saveProfileButton = findViewById(R.id.saveProfileButton);

        // Setup Blood Type Spinner
        setupBloodTypeSpinner();

        // Setup button listeners
        logoutButton.setOnClickListener(v -> logoutUser());
        editProfileButton.setOnClickListener(v -> toggleEditMode(true));
        saveProfileButton.setOnClickListener(v -> saveUserProfile());

        loadUserProfile();
    }

    private void setupBloodTypeSpinner() {
        // Create a list of blood types
        String[] bloodTypes = new String[]{"Select...", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        // Create an adapter for the spinner
        bloodTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodTypes);
        bloodTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Set the adapter to the spinner
        bloodTypeSpinner.setAdapter(bloodTypeAdapter);
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
                    medicalNotes.setText(documentSnapshot.getString("medicalNotes"));
                    emergencyContacts.setText(documentSnapshot.getString("emergencyContacts"));

                    // Set spinner selection based on saved data
                    String savedBloodType = documentSnapshot.getString("bloodType");
                    if (savedBloodType != null && !savedBloodType.isEmpty()) {
                        int spinnerPosition = bloodTypeAdapter.getPosition(savedBloodType);
                        bloodTypeSpinner.setSelection(spinnerPosition);
                    } else {
                        bloodTypeSpinner.setSelection(0); // Default to "Select..."
                    }

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
        updatedData.put("medicalNotes", medicalNotes.getText().toString().trim());
        updatedData.put("emergencyContacts", emergencyContacts.getText().toString().trim());

        // Get data from spinner
        String selectedBloodType = "";
        if (bloodTypeSpinner.getSelectedItemPosition() > 0) { // Check if a real blood type is selected
            selectedBloodType = bloodTypeSpinner.getSelectedItem().toString();
        }
        updatedData.put("bloodType", selectedBloodType);

        userDocRef.set(updatedData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                    toggleEditMode(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                    Log.e("FIRESTORE_ERROR", "Error writing document", e);
                });
    }

    private void toggleEditMode(boolean enable) {
        // Enable or disable the EditText and Spinner fields
        username.setEnabled(enable);
        contact.setEnabled(enable);
        address.setEnabled(enable);
        medicalNotes.setEnabled(enable);
        emergencyContacts.setEnabled(enable);
        bloodTypeSpinner.setEnabled(enable);

        if (enable) {
            saveProfileButton.setVisibility(View.VISIBLE);
            editProfileButton.setVisibility(View.GONE);
            username.requestFocus();
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