package com.ashu.MediSmart.repository;

import com.ashu.MediSmart.entity.ConsultationSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ConsultationSessionRepository extends MongoRepository<ConsultationSession, String> {
    Optional<ConsultationSession> findByAppointmentId(String appointmentId);
}
