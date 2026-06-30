package com.onlyfriends.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OfflineActivityRequest {
    private Long adminId;
    @NotBlank(message = "下架原因不能为空")
    private String reason;
}
