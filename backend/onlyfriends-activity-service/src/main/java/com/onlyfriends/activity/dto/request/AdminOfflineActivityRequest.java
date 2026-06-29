package com.onlyfriends.activity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminOfflineActivityRequest {
    @NotNull(message = "管理员ID不能为空")
    private Long adminId;
    @NotBlank(message = "下架原因不能为空")
    private String reason;
}
