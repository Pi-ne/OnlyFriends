package com.ququ.social.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TeamVoteResponse {
    private Long voteId;
    private Long teamId;
    private Long creatorId;
    private String creatorNickname;
    private String title;
    private String description;
    private Boolean multiple;
    private Integer status;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;
    private List<TeamVoteOptionResponse> options;
}
