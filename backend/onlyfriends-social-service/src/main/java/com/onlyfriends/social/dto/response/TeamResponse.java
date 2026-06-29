package com.onlyfriends.social.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TeamResponse {
    private Long teamId;
    private Long ownerId;
    private String ownerNickname;
    private String name;
    private String description;
    private List<String> tags;
    private Integer joinType;
    private Integer maxMembers;
    private Integer memberCount;
    private Integer status;
    private Integer score;
    private Boolean joined;
    private Integer myRole;
    private LocalDateTime createdAt;
}
