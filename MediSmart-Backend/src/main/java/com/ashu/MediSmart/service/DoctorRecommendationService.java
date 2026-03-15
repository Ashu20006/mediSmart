package com.ashu.MediSmart.service;

import com.ashu.MediSmart.DTO.UserDTO;
import com.ashu.MediSmart.entity.User;
import com.ashu.MediSmart.repository.UserRepository;
import com.ashu.MediSmart.util.AvailabilityDayUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DoctorRecommendationService {

    private final UserRepository userRepository;

    public DoctorRecommendationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserDTO> recommendDoctors(String specialty, String location, String availability) {
        String normalizedDay = AvailabilityDayUtils.normalizeAndValidateDay(availability);
        List<User> doctors = userRepository.findByRoleAndAvailabilityDay("^DOCTOR$", "^" + normalizedDay + "$");

        return doctors.stream()
                .filter(doc -> matchesTextFilter(doc.getSpecialty(), specialty))
                .filter(doc -> matchesTextFilter(doc.getLocation(), location))
                .map(doc -> new UserDTO(
                        doc.getId(),
                        doc.getName(),
                        doc.getSpecialty(),
                        doc.getLocation(),
                        doc.getYearsOfExperience(),
                        doc.getRating(),
                        AvailabilityDayUtils.toDayList(doc.getAvailability())
                ))
                .collect(Collectors.toList());
    }
    
    public List<String> getUniqueLocations() {
        List<User> allUsers = userRepository.findAll();

        List<String> uniqueLocations = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (User user : allUsers) {
            if (!"DOCTOR".equalsIgnoreCase(user.getRole()) || user.getLocation() == null) {
                continue;
            }

            String trimmedLocation = user.getLocation().trim();
            if (trimmedLocation.isEmpty()) {
                continue;
            }

            String canonicalKey = trimmedLocation.toLowerCase(Locale.ROOT);
            if (seen.add(canonicalKey)) {
                uniqueLocations.add(trimmedLocation);
            }
        }

        return uniqueLocations;
    }

    private boolean matchesTextFilter(String value, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }

        return value != null && value.trim().equalsIgnoreCase(filter.trim());
    }
}
