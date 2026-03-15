package com.ashu.MediSmart.controller;
import com.ashu.MediSmart.DTO.OtpVerificationRequest;
import com.ashu.MediSmart.DTO.PasswordResetRequest;
import com.ashu.MediSmart.DTO.OtpVerificationRequest;
import com.ashu.MediSmart.DTO.PasswordResetRequest;
import com.ashu.MediSmart.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final PasswordResetService resetService;

    public PasswordResetController(PasswordResetService resetService) {
        this.resetService = resetService;
    }

    // Step 1: Request OTP for forgot password
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }

            resetService.sendOtp(email);
            return ResponseEntity.ok(Map.of("message", "OTP sent to email!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Step 2: Verify OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerificationRequest request) {
        try {
            boolean isValid = resetService.verifyOtp(request.getEmail(), request.getOtp());
            if (isValid) {
                return ResponseEntity.ok(Map.of("message", "OTP verified successfully!"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid OTP"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Step 3: Reset password
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest request) {
        try {
            if (request.getEmail() == null || request.getOtp() == null || request.getNewPassword() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email, OTP, and new password are required"));
            }

            resetService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password reset successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
