package com.ququ.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminReviewMerchantApplyRequest {
    @NotNull(message = "管理员ID不能为空")
    private Long adminId;
    @NotNull(message = "审核结果不能为空")
    private Integer action;
    private String reason;
}
