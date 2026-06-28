package com.ququ.social.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminRestoreTeamRequest {
    @NotNull(message = "管理员ID不能为空")
    private Long adminId;
}
