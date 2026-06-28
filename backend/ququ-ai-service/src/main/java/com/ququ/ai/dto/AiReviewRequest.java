package com.ququ.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiReviewRequest {
    private Long activityId;
    private String title;
    private String description;
    private List<String> tags;
    private Integer maxParticipants;
}
