package com.ququ.user.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UserProfileResponse {
    private Long userId;
    private String email;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private LocalDate birthday;
    private String bio;
    private List<String> interestTags;
    private Integer userType;
    private Integer status;
    private Integer creditScore;
}
