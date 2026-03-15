package com.ashu.MediSmart.service;

import com.ashu.MediSmart.exception.ConsultationException;
import io.agora.media.RtcTokenBuilder2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AgoraTokenService {

    private static final Logger log = LoggerFactory.getLogger(AgoraTokenService.class);

    @Value("${agora.app-id}")
    private String appId;

    @Value("${agora.app-certificate}")
    private String appCertificate;

    public String getAppId() {
        if (appId == null || appId.isBlank()) {
            throw new ConsultationException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Consultation provider is not configured. Missing Agora App ID.");
        }
        return appId.trim();
    }

    public String generateRtcToken(String channelName, int uid, int expireSeconds) {
        if (appId == null || appId.isBlank() || appCertificate == null || appCertificate.isBlank()) {
            throw new ConsultationException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Consultation provider is not configured. Missing Agora credentials.");
        }
        if (channelName == null || channelName.isBlank()) {
            throw new ConsultationException(HttpStatus.BAD_REQUEST, "Channel name is required.");
        }
        if (uid <= 0) {
            throw new ConsultationException(HttpStatus.BAD_REQUEST, "Invalid Agora user id.");
        }
        if (expireSeconds <= 0) {
            throw new ConsultationException(HttpStatus.BAD_REQUEST, "Token expiry must be greater than zero.");
        }

        int currentTimestamp = (int) (System.currentTimeMillis() / 1000);
        int privilegeExpireTimestamp = currentTimestamp + expireSeconds;

        try {
            RtcTokenBuilder2 tokenBuilder = new RtcTokenBuilder2();
            String token = tokenBuilder.buildTokenWithUid(
                    appId.trim(),
                    appCertificate.trim(),
                    channelName.trim(),
                    uid,
                    RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                    privilegeExpireTimestamp,
                    privilegeExpireTimestamp
            );
            if (token == null || token.isBlank()) {
                throw new ConsultationException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Agora token generation returned an empty token.");
            }
            log.debug("Agora token generated for channel={} uid={} expirySeconds={}",
                    channelName, uid, expireSeconds);
            return token;
        } catch (ConsultationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Agora token generation failed for channel={} uid={}", channelName, uid, ex);
            throw new ConsultationException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to generate consultation token.", ex);
        }
    }
}
