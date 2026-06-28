package com.ququ.activity.dto.ai;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AiActivityPlanRequest {
    private String theme;
    private String locationName;
    private LocalDateTime startTime;
    private Integer durationHours;
    private Integer maxParticipants;
    private List<String> preferences;
}
