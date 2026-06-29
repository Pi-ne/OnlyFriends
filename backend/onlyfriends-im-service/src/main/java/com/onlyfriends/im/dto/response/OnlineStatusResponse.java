package com.onlyfriends.im.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnlineStatusResponse {
    private Long userId;
    private Boolean online;
}
