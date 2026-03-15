package com.ashu.MediSmart.controller;

import com.ashu.MediSmart.DTO.ConsultationJoinResponse;
import com.ashu.MediSmart.DTO.ConsultationTokenResponse;
import com.ashu.MediSmart.exception.ConsultationException;
import com.ashu.MediSmart.service.ConsultationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consultation")
public class ConsultationController {

    private static final Logger log = LoggerFactory.getLogger(ConsultationController.class);
    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @GetMapping("/{appointmentId}/join")
    public ResponseEntity<?> joinConsultation(@PathVariable String appointmentId,
                                              Authentication authentication) {
        String username = extractUsername(authentication);
        log.debug("Join consultation request received for appointmentId={} by user={}", appointmentId, username);
        ConsultationJoinResponse response = consultationService.joinConsultation(appointmentId, username);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{appointmentId}/token")
    public ResponseEntity<?> generateToken(@PathVariable String appointmentId,
                                           Authentication authentication) {
        String username = extractUsername(authentication);
        log.debug("Generate consultation token request received for appointmentId={} by user={}", appointmentId, username);
        ConsultationTokenResponse response = consultationService.generateConsultationToken(appointmentId, username);
        return ResponseEntity.ok(response);
    }

    private String extractUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ConsultationException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        return authentication.getName();
    }
}
