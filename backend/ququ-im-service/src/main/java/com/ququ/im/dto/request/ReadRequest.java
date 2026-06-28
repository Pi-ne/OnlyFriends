package com.ququ.im.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReadRequest {
    @NotNull(message = "已读消息ID不能为空")
    private Long lastReadMsgId;
}
