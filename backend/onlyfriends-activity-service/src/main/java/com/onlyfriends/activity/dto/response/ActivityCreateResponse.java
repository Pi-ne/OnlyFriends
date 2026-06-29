package com.onlyfriends.activity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityCreateResponse {
    private Long activityId;
    private Integer status;
    private String statusText;
}
