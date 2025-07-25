package com.example.emergencyapp2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.ImageButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.AdapterView;
import android.app.DatePickerDialog;
import java.util.Calendar;

public class ProfileActivity extends AppCompatActivity implements EmergencyContactsAdapter.OnContactDeleteListener, EmergencyContactsAdapter.OnContactEditListener {    private TextInputEditText username, contact, address, medicalNotes, birthday;
    private Spinner bloodTypeSpinner;
    private RadioGroup genderRadioGroup;
    private RadioButton radioMale, radioFemale;
    private Button logoutButton, editProfileButton, saveProfileButton;
    private GoogleSignInClient mGoogleSignInClient;
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
        // --- Toolbar Setup ---
        Toolbar toolbar = findViewById(R.id.profileToolbar);
        setSupportActionBar(toolbar);
        // Add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

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
        birthday = findViewById(R.id.profileBirthday);
        genderRadioGroup = findViewById(R.id.profileGenderRadioGroup);
        radioMale = findViewById(R.id.radioMale);
        radioFemale = findViewById(R.id.radioFemale);

        // Setup Blood Type Spinner
        setupBloodTypeSpinner();
        setupBirthdayPicker();

        // Setup button listeners
        logoutButton.setOnClickListener(v -> logoutUser());
        editProfileButton.setOnClickListener(v -> toggleEditMode(true));
        saveProfileButton.setOnClickListener(v -> saveUserProfile());
        addContactButton.setOnClickListener(v -> addContact());
        genderRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isDataDirty = true;
        });

        // 1. Configure Google Sign-In
        // Use the same options as in your LoginActivity
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 2. Set up the logout button click listener
        logoutButton.setOnClickListener(v -> {
            signOut();
        });

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
        birthday.addTextChangedListener(textWatcher);
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

    // Handle the back arrow click
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    private void signOut() {
        // 1. Sign out from Firebase
        mAuth.signOut();

        // 2. Sign out from Google
        // This will clear the account previously used to sign in
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // After signing out from both, navigate back to the LoginActivity
            Toast.makeText(ProfileActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();
            // FIX: Correct the context from the typo to ProfileActivity.this
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            // Clear the activity stack to prevent the user from going back to the main screen
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupBirthdayPicker() {
        birthday.setOnClickListener(v -> {
            // Only show picker if in edit mode
            if (!birthday.isEnabled()) return;

            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    ProfileActivity.this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                        birthday.setText(selectedDate);
                    },
                    year, month, day);
            datePickerDialog.show();
        });
    }

    private void setupRecyclerView() {
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Pass 'this' for both listeners
        contactsAdapter = new EmergencyContactsAdapter(emergencyContactList, this, this);
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
    public void onContactEdit(int position) {
        // Inflate the custom dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_contact, null);
        TextInputEditText editName = dialogView.findViewById(R.id.editContactName);
        TextInputEditText editNumber = dialogView.findViewById(R.id.editContactNumber);

        // Get the contact to be edited and pre-fill the fields
        EmergencyContact contactToEdit = emergencyContactList.get(position);
        editName.setText(contactToEdit.getName());
        editNumber.setText(contactToEdit.getNumber());

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Contact")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = editName.getText().toString().trim();
                    String newNumber = editNumber.getText().toString().trim();

                    if (newName.isEmpty() || newNumber.isEmpty()) {
                        Toast.makeText(this, "Name and number cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Update the contact in the list
                    contactToEdit.setName(newName);
                    contactToEdit.setNumber(newNumber);

                    // Notify the adapter that this specific item has changed
                    contactsAdapter.notifyItemChanged(position);
                    isDataDirty = true; // Mark that a change has been made
                    Toast.makeText(this, "Contact updated", Toast.LENGTH_SHORT).show();
                })
                .show();
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
                    birthday.setText(documentSnapshot.getString("birthday"));
                    address.setText(documentSnapshot.getString("address"));
                    medicalNotes.setText(documentSnapshot.getString("medicalNotes"));
                    emergencyContactList.clear(); // Clear the list before loading
                    Object contactsData = documentSnapshot.get("emergencyContacts");
                    // Check if the data is actually a List before casting
                    if (contactsData instanceof List) {
                        List<Map<String, Object>> contactsFromDb = (List<Map<String, Object>>) contactsData;
                        if (contactsFromDb != null) {
                            for (Map<String, Object> contactMap : contactsFromDb) {
                                emergencyContactList.add(new EmergencyContact(
                                        (String) contactMap.get("name"),
                                        (String) contactMap.get("number")
                                ));
                            }
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

                    String savedGender = documentSnapshot.getString("gender");
                    if (savedGender != null) {
                        if (savedGender.equals("Male")) {
                            genderRadioGroup.check(R.id.radioMale);
                        } else if (savedGender.equals("Female")) {
                            genderRadioGroup.check(R.id.radioFemale);
                        }
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
        updatedData.put("birthday", birthday.getText().toString().trim());
        updatedData.put("emergencyContacts", emergencyContactList);
        // Get data from spinner
        String selectedBloodType = "";
        if (bloodTypeSpinner.getSelectedItemPosition() > 0) { // Check if a real blood type is selected
            selectedBloodType = bloodTypeSpinner.getSelectedItem().toString();
        }
        updatedData.put("bloodType", selectedBloodType);

        String selectedGender = "";
        int selectedId = genderRadioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.radioMale) {
            selectedGender = "Male";
        } else if (selectedId == R.id.radioFemale) {
            selectedGender = "Female";
        }
        updatedData.put("gender", selectedGender); // Save gender

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
        birthday.setEnabled(enable);
        bloodTypeSpinner.setEnabled(enable);
        radioMale.setEnabled(enable);
        radioFemale.setEnabled(enable);        contactsRecyclerView.setEnabled(enable);
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