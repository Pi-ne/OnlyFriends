package com.onlyfriends.social.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeamAnnouncementResponse {
    private Long announcementId;
    private Long teamId;
    private Long publisherId;
    private String publisherNickname;
    private String title;
    private String content;
    private LocalDateTime createdAt;
}
