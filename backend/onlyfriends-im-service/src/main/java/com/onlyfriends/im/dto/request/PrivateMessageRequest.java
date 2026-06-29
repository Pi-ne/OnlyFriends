package com.onlyfriends.im.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PrivateMessageRequest {
    @NotNull(message = "接收人不能为空")
    private Long receiverId;
    private Integer msgType;
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000字")
    private String content;
}
