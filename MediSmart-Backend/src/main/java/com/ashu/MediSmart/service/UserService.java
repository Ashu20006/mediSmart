package com.ashu.MediSmart.service;

import com.ashu.MediSmart.DTO.UserDTO;
import com.ashu.MediSmart.entity.User;
import com.ashu.MediSmart.repository.UserRepository;
import com.ashu.MediSmart.util.AvailabilityDayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final Set<String> ALLOWED_ROLES = Set.of("PATIENT", "DOCTOR", "ADMIN");

    public User registerUser(UserDTO userDTO) {
        if (userDTO.getPassword() == null || !userDTO.getPassword().equals(userDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match!");
        }

        User user = new User();
        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setGender(userDTO.getGender());
        String normalizedRole = normalizeRole(userDTO.getRole());
        user.setRole(normalizedRole);

        if ("DOCTOR".equals(normalizedRole)) {
            user.setSpecialty(userDTO.getSpecialty() == null ? null : userDTO.getSpecialty().trim());
            user.setLocation(userDTO.getLocation() == null ? null : userDTO.getLocation().trim());
            user.setYearsOfExperience(userDTO.getYearsOfExperience());
            user.setRating(userDTO.getRating());
            user.setQualification(userDTO.getQualification() == null ? null : userDTO.getQualification().trim());
            user.setBio(userDTO.getBio());

            var normalizedAvailability = AvailabilityDayUtils.normalizeAndValidateDays(userDTO.getAvailability());
            if (normalizedAvailability.isEmpty()) {
                throw new IllegalArgumentException("Availability is required for doctor registration.");
            }
            user.setAvailability(AvailabilityDayUtils.toAvailabilityEntries(normalizedAvailability));
        }

        if ("PATIENT".equals(normalizedRole)) {
            if (userDTO.getAge() == null || userDTO.getAge() <= 0) {
                throw new IllegalArgumentException("Age is required for patient registration.");
            }
            user.setAge(userDTO.getAge());
        }

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role is required.");
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        return normalizedRole;
    }
}
