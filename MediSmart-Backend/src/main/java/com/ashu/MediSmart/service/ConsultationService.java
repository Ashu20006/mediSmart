package com.ashu.MediSmart.service;

import com.ashu.MediSmart.DTO.ConsultationJoinResponse;
import com.ashu.MediSmart.DTO.ConsultationTokenResponse;
import com.ashu.MediSmart.entity.Appointment;
import com.ashu.MediSmart.entity.ConsultationSession;
import com.ashu.MediSmart.entity.Status;
import com.ashu.MediSmart.entity.User;
import com.ashu.MediSmart.exception.ConsultationException;
import com.ashu.MediSmart.model.ChatMessage;
import com.ashu.MediSmart.repository.AppointmentRepository;
import com.ashu.MediSmart.repository.ConsultationSessionRepository;
import com.ashu.MediSmart.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ConsultationService {

    private static final Logger log = LoggerFactory.getLogger(ConsultationService.class);
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final int TOKEN_EXPIRY_SECONDS = 3600;

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ConsultationSessionRepository consultationSessionRepository;
    private final AgoraTokenService agoraTokenService;

    public ConsultationService(AppointmentRepository appointmentRepository,
                               UserRepository userRepository,
                               ConsultationSessionRepository consultationSessionRepository,
                               AgoraTokenService agoraTokenService) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.consultationSessionRepository = consultationSessionRepository;
        this.agoraTokenService = agoraTokenService;
    }

    public ConsultationJoinResponse joinConsultation(String appointmentId, String userEmail) {
        log.debug("Validating consultation join for appointmentId={} and user={}", appointmentId, userEmail);
        Appointment appointment = validateConsultationAccess(appointmentId, userEmail);
        ConsultationSession consultationSession = ensureActiveSession(appointment.getId());

        log.debug("Consultation join allowed for appointmentId={}, channel={}",
                appointment.getId(), consultationSession.getChannelName());
        return ConsultationJoinResponse.builder()
                .channelName(consultationSession.getChannelName())
                .build();
    }

    public ConsultationTokenResponse generateConsultationToken(String appointmentId, String userEmail) {
        log.debug("Generating consultation token for appointmentId={} and user={}", appointmentId, userEmail);
        Appointment appointment = validateConsultationAccess(appointmentId, userEmail);
        ConsultationSession consultationSession = ensureActiveSession(appointment.getId());

        int uid = ThreadLocalRandom.current().nextInt(1000, 1000000);
        String token = agoraTokenService.generateRtcToken(
                consultationSession.getChannelName(),
                uid,
                TOKEN_EXPIRY_SECONDS
        );
        if (token == null || token.isBlank()) {
            throw new ConsultationException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to generate consultation token.");
        }

        log.debug("Consultation token generated for appointmentId={}, channel={}, uid={}",
                appointment.getId(), consultationSession.getChannelName(), uid);

        return ConsultationTokenResponse.builder()
                .appId(agoraTokenService.getAppId())
                .token(token)
                .channelName(consultationSession.getChannelName())
                .uid(uid)
                .build();
    }

    public ChatMessage prepareChatMessage(String appointmentId, ChatMessage incomingMessage, String userEmail) {
        Appointment appointment = validateConsultationAccess(appointmentId, userEmail);
        User currentUser = findUserByEmail(userEmail);

        if (incomingMessage == null || incomingMessage.getMessage() == null || incomingMessage.getMessage().trim().isEmpty()) {
            throw new ConsultationException(HttpStatus.BAD_REQUEST, "Chat message cannot be empty.");
        }

        ensureActiveSession(appointment.getId());

        return ChatMessage.builder()
                .senderId(currentUser.getId())
                .senderName(currentUser.getName())
                .message(incomingMessage.getMessage().trim())
                .timestamp(LocalDateTime.now())
                .appointmentId(appointmentId)
                .build();
    }

    private ConsultationSession ensureActiveSession(String appointmentId) {
        if (appointmentId == null || appointmentId.isBlank()) {
            throw new ConsultationException(HttpStatus.BAD_REQUEST, "Appointment id is required.");
        }

        String channelName = buildChannelName(appointmentId);
        Optional<ConsultationSession> existingSession;
        try {
            existingSession = consultationSessionRepository.findByAppointmentId(appointmentId);
        } catch (DataAccessException ex) {
            log.error("Failed to read consultation session for appointmentId={}", appointmentId, ex);
            throw new ConsultationException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to load consultation session.", ex);
        }

        boolean isNewSession = existingSession.isEmpty();
        ConsultationSession session = existingSession
                .orElseGet(() -> ConsultationSession.builder()
                        .appointmentId(appointmentId)
                        .build());

        session.setChannelName(channelName);
        session.setStatus(STATUS_ACTIVE);
        if (session.getStartedAt() == null) {
            session.setStartedAt(LocalDateTime.now());
        }
        session.setEndedAt(null);

        try {
            ConsultationSession savedSession = consultationSessionRepository.save(session);
            log.debug("{} consultation session for appointmentId={} with channel={}",
                    isNewSession ? "Created" : "Updated",
                    appointmentId,
                    savedSession.getChannelName());
            return savedSession;
        } catch (DataAccessException ex) {
            log.error("Failed to persist consultation session for appointmentId={}", appointmentId, ex);
            throw new ConsultationException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to create consultation session.", ex);
        }
    }

    private Appointment validateConsultationAccess(String appointmentId, String userEmail) {
        if (appointmentId == null || appointmentId.isBlank()) {
            throw new ConsultationException(HttpStatus.BAD_REQUEST, "Appointment id is required.");
        }
        if (userEmail == null || userEmail.isBlank()) {
            throw new ConsultationException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        Appointment appointment;
        try {
            appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new ConsultationException(HttpStatus.NOT_FOUND,
                            "Appointment not found."));
        } catch (DataAccessException ex) {
            log.error("Failed to query appointment with id={}", appointmentId, ex);
            throw new ConsultationException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to load appointment details.", ex);
        }

        if (appointment.getStatus() != Status.APPROVED) {
            throw new ConsultationException(HttpStatus.FORBIDDEN,
                    "Consultation is only available for approved appointments.");
        }

        User currentUser = findUserByEmail(userEmail);
        boolean isParticipant =
                (appointment.getPatientId() != null && appointment.getPatientId().equals(currentUser.getId()))
                        || (appointment.getDoctorId() != null && appointment.getDoctorId().equals(currentUser.getId()));

        if (!isParticipant) {
            throw new ConsultationException(HttpStatus.FORBIDDEN, "Consultation access denied.");
        }

        return appointment;
    }

    private User findUserByEmail(String userEmail) {
        try {
            return userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ConsultationException(HttpStatus.FORBIDDEN,
                            "Consultation access denied."));
        } catch (DataAccessException ex) {
            log.error("Failed to query user by email={}", userEmail, ex);
            throw new ConsultationException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to validate consultation participant.", ex);
        }
    }

    private String buildChannelName(String appointmentId) {
        return "appointment_" + appointmentId.trim();
    }
}
