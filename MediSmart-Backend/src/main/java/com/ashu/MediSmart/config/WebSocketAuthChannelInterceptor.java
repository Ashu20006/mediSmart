package com.ashu.MediSmart.config;

import com.ashu.MediSmart.security.JwtUtil;
import com.ashu.MediSmart.service.TokenBlacklistService;
import com.ashu.MediSmart.service.UserDetailsServiceImpl;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    public WebSocketAuthChannelInterceptor(JwtUtil jwtUtil,
                                           UserDetailsServiceImpl userDetailsService,
                                           TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            authenticateConnectFrame(accessor);
        } else if ((StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command))
                && accessor.getUser() == null) {
            throw new AccessDeniedException("Unauthorized WebSocket action.");
        }

        return message;
    }

    private void authenticateConnectFrame(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing or invalid Authorization header.");
        }

        String token = authHeader.substring(7);
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            throw new AccessDeniedException("Token is blacklisted.");
        }

        String username = jwtUtil.extractUsername(token);
        var userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtUtil.validateToken(token, userDetails)) {
            throw new AccessDeniedException("Invalid or expired token.");
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        accessor.setUser(authentication);
    }
}
