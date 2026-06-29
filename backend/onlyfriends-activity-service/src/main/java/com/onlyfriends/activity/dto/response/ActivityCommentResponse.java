package com.onlyfriends.activity.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityCommentResponse {
    private Long commentId;
    private Long activityId;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
