package com.onlyfriends.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MerchantReviewRequest {
    private Long adminId;
    @NotNull(message = "审核结果不能为空")
    private Integer action;
    private String reason;
}
