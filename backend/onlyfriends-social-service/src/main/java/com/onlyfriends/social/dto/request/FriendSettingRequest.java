package com.onlyfriends.social.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FriendSettingRequest {
    @Size(max = 50, message = "好友备注最长50字")
    private String remark;

    @Size(max = 50, message = "好友分组最长50字")
    private String groupName;
}
