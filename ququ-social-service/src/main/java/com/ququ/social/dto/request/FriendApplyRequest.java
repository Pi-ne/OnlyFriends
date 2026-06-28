package com.ququ.social.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FriendApplyRequest {
    @Size(max = 200, message = "申请消息不能超过200字")
    private String message;
}
