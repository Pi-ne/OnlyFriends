package com.onlyfriends.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DisableTeamRequest {
    @NotBlank(message = "停用原因不能为空")
    private String reason;
}
