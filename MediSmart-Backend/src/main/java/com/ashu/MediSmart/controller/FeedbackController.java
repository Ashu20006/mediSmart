package com.ashu.MediSmart.controller;

import com.ashu.MediSmart.entity.Appointment;
import com.ashu.MediSmart.entity.Feedback;
import com.ashu.MediSmart.entity.Status;
import com.ashu.MediSmart.entity.User;
import com.ashu.MediSmart.repository.AppointmentRepository;
import com.ashu.MediSmart.repository.FeedbackRepository;
import com.ashu.MediSmart.repository.UserRepository;
import com.ashu.MediSmart.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patient/feedback")
@PreAuthorize("hasRole('PATIENT')")
public class FeedbackController {

    private final FeedbackRepository feedbackRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public FeedbackController(FeedbackRepository feedbackRepository,
                              AppointmentRepository appointmentRepository,
                              UserRepository userRepository,
                              AuthService authService) {
        this.feedbackRepository = feedbackRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<Feedback>> getMyFeedback(HttpServletRequest request) {
        Optional<User> currentUser = getCurrentUser(request);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        List<Feedback> feedback = feedbackRepository.findByPatientIdOrderByCreatedAtDesc(currentUser.get().getId());
        return ResponseEntity.ok(feedback);
    }

    @GetMapping("/appointments")
    public ResponseEntity<?> getEligibleAppointments(HttpServletRequest request) {
        Optional<User> currentUser = getCurrentUser(request);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        String patientId = currentUser.get().getId();
        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId)
                .stream()
                .filter(a -> a.getStatus() == null || a.getStatus() == Status.COMPLETED || a.getStatus() == Status.APPROVED)
                .filter(a -> a.getAppointmentDate() == null || !a.getAppointmentDate().isAfter(LocalDate.now()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(appointments);
    }

    @PostMapping
    public ResponseEntity<?> submitFeedback(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        Optional<User> currentUser = getCurrentUser(request);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        String appointmentId = payload.get("appointmentId");
        int rating;
        try {
            rating = Integer.parseInt(payload.getOrDefault("rating", "0"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Rating must be a number");
        }

        if (appointmentId == null || appointmentId.isBlank()) {
            return ResponseEntity.badRequest().body("Appointment is required");
        }
        if (rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body("Rating must be between 1 and 5");
        }

        Optional<Feedback> existing = feedbackRepository.findByAppointmentId(appointmentId);
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body("Feedback already submitted for this appointment");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElse(null);
        if (appointment == null || !appointment.getPatientId().equals(currentUser.get().getId())) {
            return ResponseEntity.status(403).body("You can only review your own appointments");
        }

        Feedback feedback = new Feedback();
        feedback.setAppointmentId(appointmentId);
        feedback.setPatientId(currentUser.get().getId());
        feedback.setPatientName(currentUser.get().getName());
        feedback.setDoctorId(appointment.getDoctorId());
        feedback.setDoctorName(appointment.getDoctorName());
        feedback.setRating(rating);
        feedback.setComment(payload.getOrDefault("comment", ""));

        Feedback saved = feedbackRepository.save(feedback);
        return ResponseEntity.ok(saved);
    }

    private Optional<User> getCurrentUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authHeader.substring(7);
        String email = authService.extractUsername(token);
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email);
    }
}
