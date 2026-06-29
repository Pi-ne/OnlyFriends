package com.onlyfriends.social.service.impl;

import com.onlyfriends.common.dto.UserBasicDTO;
import com.onlyfriends.common.exception.BizException;
import com.onlyfriends.common.response.Result;
import com.onlyfriends.common.response.ResultCode;
import com.onlyfriends.social.client.UserFeignClient;
import com.onlyfriends.social.service.UserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserClientImpl implements UserClient {
    private final UserFeignClient userFeignClient;

    @Override
    public boolean isUserValid(Long userId) {
        try {
            Result<Boolean> result = userFeignClient.isUserValid(userId);
            return success(result) && Boolean.TRUE.equals(result.getData());
        } catch (Exception ex) {
            throw new BizException(ResultCode.INTERNAL_ERROR.getCode(), "无法校验用户状态，请确认用户服务已启动");
        }
    }

    @Override
    public Map<Long, UserBasicDTO> getUsersByIds(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        try {
            Result<List<UserBasicDTO>> result = userFeignClient.getUsersByIds(userIds);
            if (!success(result) || result.getData() == null) {
                return Collections.emptyMap();
            }
            return result.getData().stream()
                    .filter(user -> user.getUserId() != null)
                    .collect(Collectors.toMap(UserBasicDTO::getUserId, Function.identity(), (left, right) -> left));
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }

    private boolean success(Result<?> result) {
        return result != null && Integer.valueOf(200).equals(result.getCode());
    }
}
