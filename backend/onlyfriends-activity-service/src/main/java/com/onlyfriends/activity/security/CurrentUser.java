package com.onlyfriends.activity.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentUser {
    private Long userId;
    private Integer userType;
    private String nickname;
}
