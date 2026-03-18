package com.ashu.MediSmart.controller;

import com.ashu.MediSmart.DTO.AppointmentRequest;
import com.ashu.MediSmart.entity.Appointment;
import com.ashu.MediSmart.DTO.PatientSummary;
import com.ashu.MediSmart.entity.Appointment;
import com.ashu.MediSmart.entity.User;
import com.ashu.MediSmart.repository.UserRepository;
import com.ashu.MediSmart.service.AppointmentService;
import com.ashu.MediSmart.service.AuthService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;
    private final AuthService authService;

    public AppointmentController(AppointmentService appointmentService,
                                 UserRepository userRepository,
                                 AuthService authService) {
        this.appointmentService = appointmentService;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    //  Only PATIENT can book appointment
    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/book")
    public ResponseEntity<Appointment> bookAppointment(@RequestBody AppointmentRequest request) {
        if (request.getAppointmentDate() == null) {
            throw new RuntimeException("Appointment date is required.");
        }
        return ResponseEntity.ok(appointmentService.bookAppointment(request));
    }

    //  Only DOCTOR can update status
    @PreAuthorize("hasRole('DOCTOR')")
    @PutMapping("/{id}/status")
    public ResponseEntity<Appointment> updateStatus(
            @PathVariable String id,
            @RequestParam String status) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, status));
    }

    // Doctor approves and schedules time (HH:mm)
    @PreAuthorize("hasRole('DOCTOR')")
    @PutMapping("/{id}/schedule")
    public ResponseEntity<Appointment> approveAndSchedule(
            @PathVariable String id,
            @RequestParam String time) {
        return ResponseEntity.ok(appointmentService.approveAndSchedule(id, time));
    }

    // Doctor reschedules date/time
    @PreAuthorize("hasRole('DOCTOR')")
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<Appointment> reschedule(
            @PathVariable String id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String time) {
        if (date == null && (time == null || time.isBlank())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(appointmentService.reschedule(id, date, time));
    }

    //get by appoinment ID not user
    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getAppointmentById(@PathVariable String id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    //get by user id
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Appointment>> getAppointmentsByPatient(@PathVariable String patientId) {
        List<Appointment> appointments = appointmentService.getAppointmentsByPatient(patientId);
        return ResponseEntity.ok(appointments);
    }
    // get by doctor
    @GetMapping("/doctor/{doctorId}")
    public List<Appointment> getAppointmentsByDoctorId(@PathVariable String doctorId) {
        return appointmentService.getAppointmentsByDoctorId(doctorId);
    }

    // Doctor fetches their patients (unique) with last visit date
    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/doctor/{doctorId}/patients")
    public ResponseEntity<?> getDoctorPatients(@PathVariable String doctorId, @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        String email = authService.extractUsername(authHeader.substring(7));
        Optional<User> current = userRepository.findByEmail(email);
        if (current.isEmpty() || !doctorId.equals(current.get().getId())) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        List<Appointment> appointments = appointmentService.getAppointmentsByDoctorId(doctorId);
        Map<String, PatientSummary> patients = appointments.stream()
                .collect(Collectors.toMap(
                        Appointment::getPatientId,
                        a -> {
                            User patient = userRepository.findById(a.getPatientId()).orElse(null);
                            return new PatientSummary(
                                    a.getPatientId(),
                                    patient != null ? patient.getName() : a.getPatientName(),
                                    patient != null ? patient.getAge() : null,
                                    patient != null ? patient.getGender() : null,
                                    patient != null ? patient.getPhoneNumber() : null,
                                    patient != null ? patient.getEmail() : null,
                                    patient != null ? patient.getLocation() : null,
                                    a.getAppointmentDate()
                            );
                        },
                        (existing, replacement) -> {
                            // keep the latest visit date
                            if (replacement.getLastVisit() != null &&
                                    (existing.getLastVisit() == null || replacement.getLastVisit().isAfter(existing.getLastVisit()))) {
                                existing.setLastVisit(replacement.getLastVisit());
                            }
                            return existing;
                        }
                ));

        return ResponseEntity.ok(patients.values());
    }

    //  Only ADMIN can view all appointments
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<Appointment>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }
}
