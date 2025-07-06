package com.example.emergencyapp2;

import java.util.List;

// This class models the user document in Firestore
public class User {
    // The field name must match the key in your Firestore document ("emergencyContacts")
    private List<EmergencyContact> emergencyContacts;

    // Firestore requires a public, no-argument constructor for deserialization
    public User() {}

    public List<EmergencyContact> getEmergencyContacts() {
        return emergencyContacts;
    }

    public void setEmergencyContacts(List<EmergencyContact> emergencyContacts) {
        this.emergencyContacts = emergencyContacts;
    }
}