package com.ququ.activity.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityRegistrationItemResponse {
    private Long registrationId;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer userType;
    private Integer status;
    private String statusText;
    private LocalDateTime registeredAt;
}
