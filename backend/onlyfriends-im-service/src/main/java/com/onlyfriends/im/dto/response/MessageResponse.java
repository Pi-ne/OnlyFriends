package com.onlyfriends.im.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MessageResponse {
    private Long msgId;
    private Long convId;
    private Integer convType;
    private Long senderId;
    private String senderNickname;
    private String senderAvatarUrl;
    private Long receiverId;
    private Long teamId;
    private Integer msgType;
    private String content;
    private Boolean mentionAll;
    private List<Long> mentionUserIds;
    private String relatedType;
    private Long relatedId;
    private Integer status;
    private Boolean mine;
    private LocalDateTime recalledAt;
    private LocalDateTime createdAt;
}
