package com.onlyfriends.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateProfileRequest {
    @Size(min = 2, max = 20, message = "昵称长度需在2-20个字符之间")
    private String nickname;
    private Integer gender;
    private LocalDate birthday;
    @Size(max = 200, message = "个性签名最长200字")
    private String bio;
    @Size(max = 10, message = "兴趣标签最多10个")
    private List<@Size(max = 20, message = "单个兴趣标签最长20字") String> interestTags;
}
