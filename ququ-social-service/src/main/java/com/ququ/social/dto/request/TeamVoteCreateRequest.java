package com.ququ.social.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TeamVoteCreateRequest {
    @NotBlank(message = "投票标题不能为空")
    @Size(max = 100, message = "投票标题不能超过100字")
    private String title;

    @Size(max = 500, message = "投票描述不能超过500字")
    private String description;

    private Boolean multiple;

    private LocalDateTime deadline;

    @NotEmpty(message = "投票选项不能为空")
    @Size(min = 2, max = 10, message = "投票选项数量必须在2到10之间")
    private List<@NotBlank(message = "投票选项不能为空") @Size(max = 100, message = "投票选项不能超过100字") String> options;
}
