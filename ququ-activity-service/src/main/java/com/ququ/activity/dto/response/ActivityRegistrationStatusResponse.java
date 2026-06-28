package com.ququ.activity.dto.response;

import lombok.Data;

@Data
public class ActivityRegistrationStatusResponse {
    private Long activityId;
    private Long userId;
    private Integer registrationStatus;
    private String registrationStatusText;
    private Integer waitlistStatus;
    private String waitlistStatusText;
    private Integer queueNo;
    private Integer currentCount;
    private Integer maxParticipants;
}
