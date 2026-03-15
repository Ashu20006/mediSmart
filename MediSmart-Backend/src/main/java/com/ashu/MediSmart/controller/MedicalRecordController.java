package com.ashu.MediSmart.controller;

import com.ashu.MediSmart.DTO.MedicalRecordRequest;
import com.ashu.MediSmart.entity.Appointment;
import com.ashu.MediSmart.entity.MedicalRecord;
import com.ashu.MediSmart.service.MedicalRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/records")
public class MedicalRecordController {

    private final MedicalRecordService recordService;

    public MedicalRecordController(MedicalRecordService recordService) {
        this.recordService = recordService;
    }

    // Doctor creates/updates medical record for an appointment
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    @PostMapping("/create")
    public ResponseEntity<Appointment> createOrUpdateRecord(@RequestBody MedicalRecordRequest request) {
        return ResponseEntity.ok(
                recordService.createOrUpdateRecord(request.getAppointmentId(), request.getNotes(), request.getPrescription())
        );
    }

    // Patient views own records
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicalRecord>> getPatientRecords(@PathVariable String patientId) {
        return ResponseEntity.ok(recordService.getPatientRecords(patientId));
    }

    // Doctor views own records
    @PreAuthorize("hasAuthority('ROLE_DOCTOR')")
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<MedicalRecord>> getDoctorRecords(@PathVariable String doctorId) {
        return ResponseEntity.ok(recordService.getDoctorRecords(doctorId));
    }

    // Admin views all records
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<MedicalRecord>> getAllRecords() {
        return ResponseEntity.ok(recordService.getAllRecords());
    }

    // Get medical record for a specific appointment
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<MedicalRecord> getRecordByAppointmentId(@PathVariable String appointmentId) {
        MedicalRecord record = recordService.getRecordByAppointmentId(appointmentId);
        if (record == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(record);
    }
}
