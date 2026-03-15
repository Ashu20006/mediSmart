package com.ashu.MediSmart.controller;

import com.ashu.MediSmart.DTO.LoginRequest;
import com.ashu.MediSmart.DTO.UserDTO;
import com.ashu.MediSmart.entity.User;
import com.ashu.MediSmart.service.AuthService;
import com.ashu.MediSmart.service.TokenBlacklistService;
import com.ashu.MediSmart.service.UserService;
import com.ashu.MediSmart.util.AvailabilityDayUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    private final TokenBlacklistService tokenBlacklistService;

    public AuthController(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    //  Logout API
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenBlacklistService.blacklistToken(token);
            return ResponseEntity.ok("User logged out successfully!");
        }

        return ResponseEntity.badRequest().body("No token found in request!");
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserDTO userDTO) {
        if (!userDTO.getPassword().equals(userDTO.getConfirmPassword())) {
            return ResponseEntity.badRequest().body("Passwords do not match!");
        }

        try {
            User savedUser = userService.registerUser(userDTO);
            return ResponseEntity.ok(savedUser);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = authService.login(request);

            // Get user details for role information
            User user = userService.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get role name directly from user
            String roleName = user.getRole() != null
                    ? user.getRole().trim().toUpperCase(Locale.ROOT)
                    : "USER";

            // Return proper JSON response
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role", roleName);
            response.put("user", Map.of(
                    "id", user.getId(),
                    "name", user.getName(),
                    "email", user.getEmail()
            ));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // Return proper JSON error response with consistent structure
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid email or password");
            errorResponse.put("error", "Authentication Failed");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = authService.extractUsername(token);

            User user = userService.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));


            // Convert User → UserDTO (include doctor fields)
            UserDTO dto = new UserDTO();
            dto.setId(user.getId());
            dto.setName(user.getName());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole() == null ? null : user.getRole().trim().toUpperCase(Locale.ROOT));
            dto.setAge(user.getAge());
            dto.setGender(user.getGender());

            // Doctor-specific fields
            dto.setSpecialty(user.getSpecialty());
            dto.setLocation(user.getLocation());
            dto.setYearsOfExperience(user.getYearsOfExperience());
            dto.setRating(user.getRating());
            dto.setAvailability(AvailabilityDayUtils.toDayList(user.getAvailability()));
            dto.setQualification(user.getQualification());
            dto.setBio(user.getBio());

            return ResponseEntity.ok(dto);
        }

        return ResponseEntity.status(401).build();
    }
}
