package com.ququ.activity.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivitySummaryResponse {
    private Long summaryId;
    private Long activityId;
    private Long creatorId;
    private String title;
    private String content;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
}
