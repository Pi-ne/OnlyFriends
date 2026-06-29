package com.onlyfriends.activity.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminReviewActivityRequest {
    @NotNull(message = "管理员ID不能为空")
    private Long adminId;
    @NotNull(message = "审核结果不能为空")
    private Integer action;
    private String comment;
}
