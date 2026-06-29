package com.onlyfriends.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotNull(message = "审核结果不能为空")
    private Integer action;
    private String comment;
}
