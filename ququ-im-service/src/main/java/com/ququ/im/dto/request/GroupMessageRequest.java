package com.ququ.im.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class GroupMessageRequest {
    @NotNull(message = "小队ID不能为空")
    private Long teamId;
    private Integer msgType;
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000字")
    private String content;
    private Boolean mentionAll;
    private List<Long> mentionUserIds;
    @Size(max = 50, message = "关联类型不能超过50字")
    private String relatedType;
    private Long relatedId;
}
