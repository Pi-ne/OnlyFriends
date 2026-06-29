package com.onlyfriends.activity.service;

import com.onlyfriends.common.dto.UserBasicDTO;

import java.util.List;

public interface UserClient {
    boolean isUserValid(Long userId);

    int getUserCredit(Long userId);

    List<UserBasicDTO> getUsersByIds(List<Long> userIds);
}
