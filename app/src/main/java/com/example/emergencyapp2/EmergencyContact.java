package com.example.emergencyapp2;

public class EmergencyContact {
    private String name;
    private String number;

    // IMPORTANT: Firestore needs a no-argument constructor for deserialization
    public EmergencyContact() {}

    public EmergencyContact(String name, String number) {
        this.name = name;
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}