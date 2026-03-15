package com.ashu.MediSmart.exception;

import com.ashu.MediSmart.controller.ConsultationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ConsultationController.class)
public class ConsultationExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ConsultationExceptionHandler.class);

    @ExceptionHandler(ConsultationException.class)
    public ResponseEntity<String> handleConsultationException(ConsultationException ex) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("Consultation operation failed with server error: {}", ex.getMessage(), ex);
        } else {
            log.warn("Consultation request rejected: status={}, message={}", ex.getStatus(), ex.getMessage());
        }
        return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Consultation access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpectedException(Exception ex) {
        log.error("Unexpected consultation error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unable to process consultation request.");
    }
}
