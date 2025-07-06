package com.example.emergencyapp2;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import android.widget.ImageButton;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.AdapterView;

public class ProfileActivity extends AppCompatActivity implements EmergencyContactsAdapter.OnContactDeleteListener {
    private TextInputEditText username, contact, address, medicalNotes;
    private Spinner bloodTypeSpinner; // Changed from EditText
    private Button logoutButton, editProfileButton, saveProfileButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DocumentReference userDocRef;
    private ArrayAdapter<String> bloodTypeAdapter;
    private ImageButton contactsInfoButton;
    private RecyclerView contactsRecyclerView;
    private TextInputEditText newContactNameEditText, newContactNumberEditText;
    private TextInputLayout newContactNameLayout, newContactNumberLayout;
    private Button addContactButton;
    private EmergencyContactsAdapter contactsAdapter;
    private List<EmergencyContact> emergencyContactList = new ArrayList<>();
    private boolean isDataDirty = false;


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
        bloodTypeSpinner = findViewById(R.id.profileBloodTypeSpinner); // New Spinner
        logoutButton = findViewById(R.id.logoutButton);
        editProfileButton = findViewById(R.id.editProfileButton);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        contactsRecyclerView = findViewById(R.id.contactsRecyclerView);
        newContactNameEditText = findViewById(R.id.newContactNameEditText);
        newContactNumberEditText = findViewById(R.id.newContactNumberEditText);
        addContactButton = findViewById(R.id.addContactButton);
        newContactNameLayout = findViewById(R.id.newContactNameLayout);
        newContactNumberLayout = findViewById(R.id.newContactNumberLayout);


        // Setup Blood Type Spinner
        setupBloodTypeSpinner();

        // Setup button listeners
        logoutButton.setOnClickListener(v -> logoutUser());
        editProfileButton.setOnClickListener(v -> toggleEditMode(true));
        saveProfileButton.setOnClickListener(v -> saveUserProfile());
        addContactButton.setOnClickListener(v -> addContact());
        setupRecyclerView(); // Call a new method to set up the list

        loadUserProfile();

        toggleEditMode(false);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                isDataDirty = true;
            }
        };

        username.addTextChangedListener(textWatcher);
        contact.addTextChangedListener(textWatcher);
        address.addTextChangedListener(textWatcher);
        medicalNotes.addTextChangedListener(textWatcher);
        newContactNameEditText.addTextChangedListener(textWatcher);
        newContactNumberEditText.addTextChangedListener(textWatcher);

        // In ProfileActivity.java, at the end of onCreate()

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Check if we are in edit mode and if there are unsaved changes
                boolean isInEditMode = editProfileButton.getVisibility() == View.GONE;

                if (isInEditMode && isDataDirty) {
                    // Show a confirmation dialog
                    new MaterialAlertDialogBuilder(ProfileActivity.this)
                            .setTitle("Discard Changes?")
                            .setMessage("You have unsaved changes. Are you sure you want to go back?")
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                // User wants to stay, so just dismiss the dialog
                                dialog.dismiss();
                            })
                            .setPositiveButton("Discard", (dialog, which) -> {
                                // User wants to discard, so finish the activity
                                finish();
                            })
                            .show();
                } else {
                    // If not in edit mode or no changes, proceed with default back behavior
                    finish();
                }
            }
        });
    }

    private void setupRecyclerView() {
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // The 'this' refers to the OnContactDeleteListener that the activity now implements
        contactsAdapter = new EmergencyContactsAdapter(emergencyContactList, this);
        contactsRecyclerView.setAdapter(contactsAdapter);
    }
    private void addContact() {
        String name = newContactNameEditText.getText().toString().trim();
        String number = newContactNumberEditText.getText().toString().trim();

        if (name.isEmpty() || number.isEmpty()) {
            Toast.makeText(this, "Please enter both name and number", Toast.LENGTH_SHORT).show();
            return;
        }

        emergencyContactList.add(new EmergencyContact(name, number));
        // Notify the adapter that a new item was inserted at the end of the list
        contactsAdapter.notifyItemInserted(emergencyContactList.size() - 1);

        // Clear the input fields for the next entry
        newContactNameEditText.setText("");
        newContactNumberEditText.setText("");
        newContactNameEditText.requestFocus();
        isDataDirty = true;
    }

    @Override
    public void onContactDelete(int position) {
        // Get the contact to show its name in the dialog for better context
        EmergencyContact contactToDelete = emergencyContactList.get(position);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Contact?")
                .setMessage("Are you sure you want to delete " + contactToDelete.getName() + "?")
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User clicked Cancel, so just dismiss the dialog.
                    dialog.dismiss();
                })
                .setPositiveButton("Delete", (dialog, which) -> {
                    // User confirmed the deletion, so proceed.
                    emergencyContactList.remove(position);
                    contactsAdapter.notifyItemRemoved(position);
                    Toast.makeText(this, "Contact removed", Toast.LENGTH_SHORT).show();
                    isDataDirty = true; // Mark that a change has been made
                })
                .show();
    }

    private void showContactsInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Emergency Contacts Info")
                .setMessage("• Enter one phone number per line.\n\n• You can use formats like 012-3456789 or +60123456789.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void setupBloodTypeSpinner() {
        // Create a list of blood types
        String[] bloodTypes = new String[]{"Select...", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        // Create an adapter for the spinner
        bloodTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodTypes);
        bloodTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Set the adapter to the spinner
        bloodTypeSpinner.setAdapter(bloodTypeAdapter);

        bloodTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean isInitialSelection = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitialSelection) {
                    isInitialSelection = false; // Ignore the first automatic selection
                } else {
                    isDataDirty = true; // Any subsequent user selection marks data as dirty
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Not needed
            }
        });
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
                    emergencyContactList.clear(); // Clear the list before loading
                    List<Map<String, Object>> contactsFromDb = (List<Map<String, Object>>) documentSnapshot.get("emergencyContacts");
                    if (contactsFromDb != null) {
                        for (Map<String, Object> contactMap : contactsFromDb) {
                            emergencyContactList.add(new EmergencyContact(
                                    (String) contactMap.get("name"),
                                    (String) contactMap.get("number")
                            ));
                        }
                    }
                    contactsAdapter.notifyDataSetChanged(); // Refresh the entire list view

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
        String newName = newContactNameEditText.getText().toString().trim();
        String newNumber = newContactNumberEditText.getText().toString().trim();

        // If both fields have text, add the contact to our list automatically
        if (!newName.isEmpty() && !newNumber.isEmpty()) {
            emergencyContactList.add(new EmergencyContact(newName, newNumber));
            contactsAdapter.notifyItemInserted(emergencyContactList.size() - 1);
            // Clear the fields so it doesn't get added again
            newContactNameEditText.setText("");
            newContactNumberEditText.setText("");
        }

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("username", username.getText().toString().trim());
        updatedData.put("contact", contact.getText().toString().trim());
        updatedData.put("address", address.getText().toString().trim());
        updatedData.put("medicalNotes", medicalNotes.getText().toString().trim());
        updatedData.put("emergencyContacts", emergencyContactList);
        // Get data from spinner
        String selectedBloodType = "";
        if (bloodTypeSpinner.getSelectedItemPosition() > 0) { // Check if a real blood type is selected
            selectedBloodType = bloodTypeSpinner.getSelectedItem().toString();
        }
        updatedData.put("bloodType", selectedBloodType);

        userDocRef.set(updatedData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                    isDataDirty = false;
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
        bloodTypeSpinner.setEnabled(enable);
        contactsRecyclerView.setEnabled(enable);
        newContactNameEditText.setEnabled(enable);
        newContactNumberEditText.setEnabled(enable);
        addContactButton.setEnabled(enable);
        if (contactsAdapter != null) {
            contactsAdapter.setEditMode(enable);
        }

        findViewById(R.id.addContactLabel).setVisibility(enable ? View.VISIBLE : View.GONE);
        newContactNameLayout.setVisibility(enable ? View.VISIBLE : View.GONE);
        newContactNumberLayout.setVisibility(enable ? View.VISIBLE : View.GONE);
        addContactButton.setVisibility(enable ? View.VISIBLE : View.GONE);

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