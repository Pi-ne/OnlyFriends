package com.ququ.activity.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityCheckinResponse {
    private Long activityId;
    private Long userId;
    private Long checkinId;
    private Boolean checkedIn;
    private LocalDateTime checkinTime;
}
