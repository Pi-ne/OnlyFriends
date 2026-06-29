package com.onlyfriends.social.dto.response;

import lombok.Data;

@Data
public class TeamScoreResponse {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer role;
    private Integer score;
}
