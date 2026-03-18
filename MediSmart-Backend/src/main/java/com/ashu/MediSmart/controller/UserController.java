package com.ashu.MediSmart.controller;

import com.ashu.MediSmart.DTO.UserDTO;
import com.ashu.MediSmart.entity.User;
import com.ashu.MediSmart.repository.UserRepository;
import com.ashu.MediSmart.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository,
                          AuthService authService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UserDTO payload, HttpServletRequest request) {
        Optional<User> currentUser = extractUser(request);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        User user = currentUser.get();
        user.setName(payload.getName() != null ? payload.getName() : user.getName());
        user.setPhoneNumber(payload.getPhoneNumber());
        user.setGender(payload.getGender());
        user.setAge(payload.getAge());
        user.setLocation(payload.getLocation());

        userRepository.save(user);

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setAge(user.getAge());
        dto.setGender(user.getGender());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setLocation(user.getLocation());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        Optional<User> currentUser = extractUser(request);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        User user = currentUser.get();
        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");

        if (currentPassword == null || newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("Invalid password payload");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.status(400).body("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok("Password updated");
    }

    private Optional<User> extractUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }

        String token = authHeader.substring(7);
        String username = authService.extractUsername(token);
        if (username == null) {
            return Optional.empty();
        }

        return userRepository.findByEmail(username);
    }
}
