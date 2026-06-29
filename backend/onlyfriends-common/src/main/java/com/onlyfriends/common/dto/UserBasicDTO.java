package com.onlyfriends.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicDTO {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer userType;
}
