package com.ashu.MediSmart.entity;

import java.time.LocalDateTime;

public class MedicalRecord {

    private String notes;

    private String prescription;

    private LocalDateTime createdAt;

    public MedicalRecord() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPrescription() {
        return prescription;
    }

    public void setPrescription(String prescription) {
        this.prescription = prescription;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}