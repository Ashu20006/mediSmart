package com.ashu.MediSmart.service;

import com.ashu.MediSmart.DTO.AppointmentRequest;
import com.ashu.MediSmart.entity.Appointment;
import com.ashu.MediSmart.entity.Status;
import com.ashu.MediSmart.repository.AppointmentRepository;
import com.ashu.MediSmart.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public AppointmentService(AppointmentRepository appointmentRepository, UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    // Patient books appointment
    public Appointment bookAppointment(AppointmentRequest request) {
        // Verify patient exists and get patient details
        var patient = userRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        
        // Verify doctor exists and get doctor details
        var doctor = userRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        Appointment appointment = new Appointment();
        appointment.setPatientId(request.getPatientId());
        appointment.setPatientName(patient.getName()); // Snapshot of patient name
        appointment.setDoctorId(request.getDoctorId());
        appointment.setDoctorName(doctor.getName());   // Snapshot of doctor name
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setReason(request.getReason());
        appointment.setStatus(Status.PENDING);

        return appointmentRepository.save(appointment);
    }

    // Doctor updates status only
    public Appointment updateStatus(String appointmentId, String status) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        try {
            appointment.setStatus(Status.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status);
        }
        return appointmentRepository.save(appointment);
    }

    // Doctor approves and schedules time
    public Appointment approveAndSchedule(String appointmentId, String time) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getAppointmentDate() == null) {
            throw new RuntimeException("Appointment date missing.");
        }
        if (time == null || time.isBlank()) {
            throw new RuntimeException("Appointment time is required.");
        }

        LocalTime parsedTime;
        try {
            parsedTime = LocalTime.parse(time);
        } catch (Exception ex) {
            throw new RuntimeException("Invalid time format. Use HH:mm");
        }

        appointment.setAppointmentTime(parsedTime);
        appointment.setStatus(Status.APPROVED);
        return appointmentRepository.save(appointment);
    }

    // Doctor can update date/time together if needed
    public Appointment reschedule(String appointmentId, LocalDate date, String time) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (date != null) {
            appointment.setAppointmentDate(date);
        }
        if (time != null && !time.isBlank()) {
            try {
                appointment.setAppointmentTime(LocalTime.parse(time));
            } catch (Exception ex) {
                throw new RuntimeException("Invalid time format. Use HH:mm");
            }
        }
        return appointmentRepository.save(appointment);
    }

    public Appointment getAppointmentById(String id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found with id: " + id));
    }

    public List<Appointment> getAppointmentsByDoctorId(String doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    public List<Appointment> getAppointmentsByPatient(String patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    // Admin views all appointments
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }
}
