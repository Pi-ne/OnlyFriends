package com.ququ.admin.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BanUserRequest {
    @NotBlank(message = "封禁原因不能为空")
    private String reason;
    @NotNull(message = "封禁期限不能为空")
    @Future(message = "封禁期限必须晚于当前时间")
    private LocalDateTime banExpireAt;
}
