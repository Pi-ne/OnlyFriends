package com.onlyfriends.social.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendApplyResponse {
    private Long applyId;
    private Long applicantId;
    private String applicantNickname;
    private Long targetId;
    private String targetNickname;
    private String message;
    private Integer status;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
