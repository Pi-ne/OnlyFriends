package com.onlyfriends.im.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationResponse {
    private Long convId;
    private Integer convType;
    private Long peerUserId;
    private String peerNickname;
    private String peerAvatarUrl;
    private Long teamId;
    private String title;
    private Long lastMsgId;
    private String lastMsgPreview;
    private LocalDateTime lastMsgAt;
    private Long unreadCount;
}
