package com.onlyfriends.social.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TeamVoteCastRequest {
    @NotEmpty(message = "投票选项不能为空")
    @Size(max = 10, message = "投票选项数量不能超过10个")
    private List<Long> optionIds;
}
