package com.ashu.MediSmart.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "consultations")
public class ConsultationSession {

    @Id
    private String id;

    private String appointmentId;
    private String channelName;
    private String status; // ACTIVE, ENDED
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
