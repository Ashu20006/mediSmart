package com.ashu.MediSmart.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "password_resets")
public class PasswordResetToken {
    @Id
    private String id;

    private String email;
    private String otp;
    private LocalDateTime expiry;

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
    public LocalDateTime getExpiry() { return expiry; }
    public void setExpiry(LocalDateTime expiry) { this.expiry = expiry; }
}
