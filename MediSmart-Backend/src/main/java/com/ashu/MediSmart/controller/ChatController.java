package com.ashu.MediSmart.controller;

import com.ashu.MediSmart.model.ChatMessage;
import com.ashu.MediSmart.service.ConsultationService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ConsultationService consultationService;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          ConsultationService consultationService) {
        this.messagingTemplate = messagingTemplate;
        this.consultationService = consultationService;
    }

    @MessageMapping("/consultation/{appointmentId}")
    public void sendMessage(@DestinationVariable String appointmentId,
                            ChatMessage message,
                            Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new AccessDeniedException("Authentication required for chat.");
        }

        ChatMessage outboundMessage = consultationService.prepareChatMessage(
                appointmentId,
                message,
                principal.getName()
        );

        messagingTemplate.convertAndSend("/topic/consultation/" + appointmentId, outboundMessage);
    }
}
