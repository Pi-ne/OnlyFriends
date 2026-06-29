package com.onlyfriends.social.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TeamAnnouncementRequest {
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 100, message = "公告标题不能超过100字")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    @Size(max = 1000, message = "公告内容不能超过1000字")
    private String content;
}
