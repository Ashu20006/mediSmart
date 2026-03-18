package com.ashu.MediSmart.repository;

import com.ashu.MediSmart.entity.Feedback;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends MongoRepository<Feedback, String> {
    List<Feedback> findByPatientIdOrderByCreatedAtDesc(String patientId);
    List<Feedback> findByDoctorIdOrderByCreatedAtDesc(String doctorId);
    Optional<Feedback> findByAppointmentId(String appointmentId);
}
