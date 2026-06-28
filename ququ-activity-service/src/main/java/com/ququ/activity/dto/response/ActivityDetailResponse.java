package com.ququ.activity.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityDetailResponse {
    private Long activityId;
    private Long creatorId;
    private String creatorNickname;
    private String creatorAvatarUrl;
    private Integer creatorUserType;
    private String title;
    private String description;
    private List<String> tags;
    private String coverUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime regDeadline;
    private String locationName;
    private BigDecimal locationLat;
    private BigDecimal locationLng;
    private String locationDetail;
    private Integer maxParticipants;
    private Integer currentCount;
    private BigDecimal fee;
    private Integer status;
    private String statusText;
    private Integer reviewType;
    private Boolean locationVerify;
    private Integer locationRadius;
    private Long templateId;
}
