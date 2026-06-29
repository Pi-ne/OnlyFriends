package com.onlyfriends.social.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeamJoinApplyResponse {
    private Long applyId;
    private Long teamId;
    private Long userId;
    private String nickname;
    private String message;
    private Integer status;
    private String rejectReason;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
