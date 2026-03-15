package com.ashu.MediSmart.repository;

import com.ashu.MediSmart.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    @Query("{ 'role': { $regex: ?0, $options: 'i' }, 'availability.day': { $regex: ?1, $options: 'i' } }")
    List<User> findByRoleAndAvailabilityDay(String role, String day);

    // Find all locations of doctors
    @Query(value = "{ }", fields = "{ 'location': 1 }")
    List<User> findDistinctDoctorLocations();

}
