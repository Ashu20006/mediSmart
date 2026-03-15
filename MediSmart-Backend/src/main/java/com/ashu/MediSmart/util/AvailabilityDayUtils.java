package com.ashu.MediSmart.util;

import com.ashu.MediSmart.entity.User;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class AvailabilityDayUtils {

    private static final Set<String> VALID_DAYS = Set.of(
            "MONDAY",
            "TUESDAY",
            "WEDNESDAY",
            "THURSDAY",
            "FRIDAY",
            "SATURDAY",
            "SUNDAY"
    );

    private AvailabilityDayUtils() {
    }

    public static String normalizeAndValidateDay(String day) {
        if (day == null || day.isBlank()) {
            throw new IllegalArgumentException("Availability day is required.");
        }

        String normalized = day.trim().toUpperCase(Locale.ROOT);
        if (!VALID_DAYS.contains(normalized)) {
            throw new IllegalArgumentException("Invalid availability day: " + day);
        }

        return normalized;
    }

    public static List<String> normalizeAndValidateDays(List<String> days) {
        if (days == null || days.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> uniqueDays = new LinkedHashSet<>();
        for (String day : days) {
            uniqueDays.add(normalizeAndValidateDay(day));
        }

        return new ArrayList<>(uniqueDays);
    }

    public static List<User.Availability> toAvailabilityEntries(List<String> days) {
        if (days == null || days.isEmpty()) {
            return List.of();
        }

        return days.stream()
                .map(User.Availability::new)
                .collect(Collectors.toList());
    }

    public static List<String> toDayList(List<User.Availability> availability) {
        if (availability == null || availability.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> uniqueDays = new LinkedHashSet<>();
        for (User.Availability slot : availability) {
            if (slot == null || slot.getDay() == null) {
                continue;
            }

            String day = slot.getDay().trim();
            if (!day.isEmpty()) {
                uniqueDays.add(day.toUpperCase(Locale.ROOT));
            }
        }

        return new ArrayList<>(uniqueDays);
    }
}
