package com.ququ.user.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminBanUserRequest {
    @NotNull(message = "管理员ID不能为空")
    private Long adminId;
    @NotBlank(message = "封禁原因不能为空")
    private String reason;
    @NotNull(message = "封禁期限不能为空")
    @Future(message = "封禁期限必须晚于当前时间")
    private LocalDateTime banExpireAt;
}
