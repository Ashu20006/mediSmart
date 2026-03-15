package com.ashu.MediSmart.repository;

import com.ashu.MediSmart.entity.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {
    Optional<PasswordResetToken> findByEmailAndOtp(String email, String otp);
    void deleteByEmail(String email);
}
