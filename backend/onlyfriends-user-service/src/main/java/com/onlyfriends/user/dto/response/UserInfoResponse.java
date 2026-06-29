package com.onlyfriends.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer userType;
    private Integer creditScore;
}
