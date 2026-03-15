package com.ashu.MediSmart.service;

import com.ashu.MediSmart.entity.Appointment;
import com.ashu.MediSmart.entity.MedicalRecord;
import com.ashu.MediSmart.repository.AppointmentRepository;
import com.ashu.MediSmart.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicalRecordService {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;

    public MedicalRecordService(UserRepository userRepository,
                                AppointmentRepository appointmentRepository) {
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
    }

    // Doctor creates/updates medical record for an appointment
    public Appointment createOrUpdateRecord(String appointmentId, String notes, String prescription) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        MedicalRecord record = new MedicalRecord();
        record.setNotes(notes);
        record.setPrescription(prescription);

        appointment.setMedicalRecord(record);
        return appointmentRepository.save(appointment);
    }

    // Patient views own medical records via their appointments
    public List<MedicalRecord> getPatientRecords(String patientId) {
        // Get all appointments for this patient
        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);
        
        // Extract medical records from appointments that have them
        return appointments.stream()
                .map(Appointment::getMedicalRecord)
                .filter(record -> record != null)
                .toList();
    }

    // Doctor views medical records of their patients
    public List<MedicalRecord> getDoctorRecords(String doctorId) {
        // Get all appointments for this doctor
        List<Appointment> appointments = appointmentRepository.findByDoctorId(doctorId);
        
        // Extract medical records from appointments that have them
        return appointments.stream()
                .map(Appointment::getMedicalRecord)
                .filter(record -> record != null)
                .toList();
    }

    // Admin views all medical records
    public List<MedicalRecord> getAllRecords() {
        return appointmentRepository.findAll().stream()
                .map(Appointment::getMedicalRecord)
                .filter(record -> record != null)
                .toList();
    }

    // Get medical record for a specific appointment
    public MedicalRecord getRecordByAppointmentId(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        return appointment.getMedicalRecord();
    }
}
