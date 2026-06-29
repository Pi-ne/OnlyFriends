package com.onlyfriends.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminLoginResponse {
    private String accessToken;
    private Long expiresIn;
    private Long adminId;
    private String username;
    private String nickname;
}
