package com.onlyfriends.social.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TeamCreateRequest {
    @NotBlank(message = "小队名称不能为空")
    @Size(max = 100, message = "小队名称不能超过100字")
    private String name;
    @Size(max = 500, message = "小队介绍不能超过500字")
    private String description;
    @Size(max = 8, message = "标签最多8个")
    private List<@Size(max = 20, message = "单个标签不能超过20字") String> tags;
    @NotNull(message = "加入方式不能为空")
    @Min(value = 0, message = "加入方式不合法")
    @Max(value = 1, message = "加入方式不合法")
    private Integer joinType;
    @NotNull(message = "人数上限不能为空")
    @Min(value = 1, message = "人数上限至少为1")
    private Integer maxMembers;
}
