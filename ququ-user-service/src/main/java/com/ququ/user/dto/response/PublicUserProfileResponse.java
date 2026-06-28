package com.ququ.user.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class PublicUserProfileResponse {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private String bio;
    private List<String> interestTags;
    private Integer userType;
    private Boolean isFriend = false;
    private Boolean isFollowing = false;
}
