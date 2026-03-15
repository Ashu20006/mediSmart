package com.ashu.MediSmart.service;

import com.ashu.MediSmart.entity.PasswordResetToken;
import com.ashu.MediSmart.entity.User;
import com.ashu.MediSmart.repository.PasswordResetTokenRepository;
import com.ashu.MediSmart.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
    }

    // Send OTP to user's email
    public void sendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String otp = String.valueOf(new Random().nextInt(900000) + 100000); // 6-digit OTP

        // Save OTP in DB with 10 min expiry
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(email);
        token.setOtp(otp);
        token.setExpiry(LocalDateTime.now().plusMinutes(10));
        tokenRepository.save(token);

        // Send OTP email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Reset OTP");
        message.setText("Your OTP is: " + otp + "\nIt is valid for 10 minutes.");
        mailSender.send(message);
    }

    // Verify OTP
    public boolean verifyOtp(String email, String otp) {
        PasswordResetToken token = tokenRepository.findByEmailAndOtp(email, otp)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP"));

        if (token.getExpiry().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has expired");
        }
        return true;
    }

    // Reset password after OTP verification
    public void resetPassword(String email, String otp, String newPassword) {
        PasswordResetToken token = tokenRepository.findByEmailAndOtp(email, otp)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP"));

        if (token.getExpiry().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(token);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has expired");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Encode new password and save
        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        userRepository.save(user);

        // Delete used OTP token
        tokenRepository.delete(token);
    }
}
