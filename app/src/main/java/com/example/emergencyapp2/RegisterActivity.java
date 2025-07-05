package com.example.emergencyapp2;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText username, email, password, contact;
    private Button registerButton, birthdatePickerButton;
    private RadioGroup genderRadioGroup;
    private TextView goToLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String selectedBirthdate = ""; // To store the selected birthdate

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
        contact = findViewById(R.id.registerContact);
        genderRadioGroup = findViewById(R.id.genderRadioGroup);
        registerButton = findViewById(R.id.registerButton);
        birthdatePickerButton = findViewById(R.id.birthdatePickerButton);
        goToLogin = findViewById(R.id.goToLogin);

        // Listeners
        registerButton.setOnClickListener(v -> createAccount());
        goToLogin.setOnClickListener(v -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
        birthdatePickerButton.setOnClickListener(v -> showDatePickerDialog());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, monthOfYear, dayOfMonth) -> {
            selectedBirthdate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
            birthdatePickerButton.setText("Birthdate: " + selectedBirthdate);
        }, year, month, day);
        datePickerDialog.show();
    }

    private String getAge(int year, int month, int day) {
        Calendar dob = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        dob.set(year, month, day);
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return String.valueOf(age);
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
                        String userId = mAuth.getCurrentUser().getUid();
                        saveUserData(userId);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserData(String userId) {
        // Get Optional Data
        String contactStr = contact.getText().toString().trim();
        int selectedGenderId = genderRadioGroup.getCheckedRadioButtonId();
        String genderStr = "";
        if (selectedGenderId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedGenderId);
            genderStr = selectedRadioButton.getText().toString();
        }

        String ageStr = "";
        if (!selectedBirthdate.isEmpty()) {
            String[] dateParts = selectedBirthdate.split("/");
            ageStr = getAge(Integer.parseInt(dateParts[2]), Integer.parseInt(dateParts[1]), Integer.parseInt(dateParts[0]));
        }

        // Create Map to store data
        Map<String, Object> user = new HashMap<>();
        user.put("username", username.getText().toString().trim());
        user.put("email", email.getText().toString().trim());
        // Save optional data, even if it's empty
        user.put("contact", contactStr);
        user.put("age", ageStr);
        user.put("gender", genderStr);
        user.put("birthdate", selectedBirthdate);
        // Add empty fields for profile section
        user.put("address", "");
        user.put("bloodType", "");
        user.put("medicalNotes", "");
        user.put("emergencyContacts", "");

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    startActivity(new Intent(RegisterActivity.this, MapsActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, "Error saving user data.", Toast.LENGTH_SHORT).show());
    }
}