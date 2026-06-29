package com.onlyfriends.activity.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminActivityResponse {
    private Long activityId;
    private Long creatorId;
    private String title;
    private Integer status;
    private String statusText;
    private Integer reviewType;
    private LocalDateTime startTime;
    private LocalDateTime regDeadline;
    private LocalDateTime createdAt;
}
