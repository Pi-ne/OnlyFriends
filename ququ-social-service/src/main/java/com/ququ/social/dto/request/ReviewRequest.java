package com.ququ.social.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotNull(message = "审核结果不能为空")
    private Integer action;
    @Size(max = 200, message = "原因不能超过200字")
    private String reason;
}
