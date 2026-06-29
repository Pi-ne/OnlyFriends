package com.onlyfriends.social.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeamFileResponse {
    private Long fileId;
    private Long teamId;
    private Long userId;
    private String nickname;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private LocalDateTime createdAt;
}
