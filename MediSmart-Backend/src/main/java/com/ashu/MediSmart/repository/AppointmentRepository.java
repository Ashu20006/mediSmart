package com.ashu.MediSmart.repository;

import com.ashu.MediSmart.entity.Appointment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AppointmentRepository extends MongoRepository<Appointment, String> {

    List<Appointment> findByPatientId(String patientId);

    List<Appointment> findByDoctorId(String doctorId);
}
