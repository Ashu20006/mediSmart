package com.ashu.MediSmart.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationTokenResponse {
    private String appId;
    private String token;
    private String channelName;
    private int uid;
}
