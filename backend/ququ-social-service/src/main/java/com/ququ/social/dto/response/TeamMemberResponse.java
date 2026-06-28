package com.ququ.social.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeamMemberResponse {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer userType;
    private Integer role;
    private Integer score;
    private LocalDateTime joinedAt;
}
