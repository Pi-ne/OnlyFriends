package com.onlyfriends.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiImageClassifyRequest {
    private Long activityId;
    private List<String> imageUrls;
}
