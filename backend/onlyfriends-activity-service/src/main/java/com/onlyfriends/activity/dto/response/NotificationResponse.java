package com.onlyfriends.activity.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long notificationId;
    private String type;
    private String title;
    private String content;
    private String relatedType;
    private Long relatedId;
    private Boolean read;
    private LocalDateTime createdAt;
}
