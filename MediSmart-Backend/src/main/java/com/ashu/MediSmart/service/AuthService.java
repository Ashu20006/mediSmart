package com.ashu.MediSmart.service;

import com.ashu.MediSmart.DTO.LoginRequest;
import com.ashu.MediSmart.entity.User;
import com.ashu.MediSmart.repository.UserRepository;
import com.ashu.MediSmart.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public String extractUsername(String token) {
        return jwtUtil.extractUsername(token);
    }

    public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    public String login(LoginRequest request) {
        try {
            // Let Spring Security handle authentication
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // If successful, get the user and extract role
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get the role directly from the user object
            String role = user.getRole() != null
                    ? user.getRole().trim().toUpperCase(Locale.ROOT)
                    : "USER";
            
            return jwtUtil.generateToken(userDetails.getUsername(), role);

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password", e);
        }
    }
}
