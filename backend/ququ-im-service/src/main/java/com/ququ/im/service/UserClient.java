package com.ququ.im.service;

import com.ququ.common.dto.UserBasicDTO;

import java.util.List;
import java.util.Map;

public interface UserClient {
    boolean isUserValid(Long userId);

    Map<Long, UserBasicDTO> getUsersByIds(List<Long> userIds);
}
