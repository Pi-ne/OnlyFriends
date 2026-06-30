package com.onlyfriends.user.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserResponse {
    private Long userId;
    private String email;
    private String wxOpenid;
    private String nickname;
    private String avatarUrl;
    private Integer userType;
    private Integer status;
    private Integer creditScore;
    private LocalDateTime banExpireAt;
    private LocalDateTime createdAt;
}
