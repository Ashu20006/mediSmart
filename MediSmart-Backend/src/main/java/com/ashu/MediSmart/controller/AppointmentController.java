package com.ashu.MediSmart.controller;

import com.ashu.MediSmart.DTO.AppointmentRequest;
import com.ashu.MediSmart.entity.Appointment;
import com.ashu.MediSmart.service.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
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

    //  Only ADMIN can view all appointments
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<Appointment>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }
}
