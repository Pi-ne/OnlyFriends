package com.ququ.activity.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityListItemResponse {
    private Long activityId;
    private String title;
    private String coverUrl;
    private List<String> tags;
    private LocalDateTime startTime;
    private String locationName;
    private BigDecimal locationLat;
    private BigDecimal locationLng;
    private String locationDetail;
    private Long distanceMeters;
    private Integer currentCount;
    private Integer maxParticipants;
    private BigDecimal fee;
    private Integer status;
    private String statusText;
}
