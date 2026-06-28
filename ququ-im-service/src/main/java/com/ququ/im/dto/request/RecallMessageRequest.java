package com.ququ.im.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecallMessageRequest {
    @NotNull(message = "消息ID不能为空")
    private Long msgId;
}
