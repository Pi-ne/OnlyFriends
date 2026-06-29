package com.onlyfriends.social.dto.response;

import lombok.Data;

@Data
public class UserRelationResponse {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer userType;
    private String remark;
    private String groupName;
    private Boolean mutualFollow;
    private Boolean friend;
}
