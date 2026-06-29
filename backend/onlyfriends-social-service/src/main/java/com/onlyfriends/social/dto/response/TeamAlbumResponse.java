package com.onlyfriends.social.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeamAlbumResponse {
    private Long albumId;
    private Long teamId;
    private Long userId;
    private String nickname;
    private String imageUrl;
    private String description;
    private LocalDateTime createdAt;
}
