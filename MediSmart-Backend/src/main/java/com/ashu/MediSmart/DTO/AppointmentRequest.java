package com.ashu.MediSmart.DTO;

import java.time.LocalDate;

public class AppointmentRequest {
    private String patientId;
    private String doctorId;
    private LocalDate appointmentDate;
    private String reason;

    // Getters & Setters
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
