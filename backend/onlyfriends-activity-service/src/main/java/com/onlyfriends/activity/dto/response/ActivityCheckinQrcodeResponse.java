package com.onlyfriends.activity.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityCheckinQrcodeResponse {
    private Long activityId;
    private String qrcodeContent;
    private LocalDateTime generatedAt;
    private LocalDateTime expiresAt;
}
