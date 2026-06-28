package com.ququ.social.dto.response;

import lombok.Data;

@Data
public class TeamVoteOptionResponse {
    private Long optionId;
    private String content;
    private Integer voteCount;
    private Boolean selected;
}
