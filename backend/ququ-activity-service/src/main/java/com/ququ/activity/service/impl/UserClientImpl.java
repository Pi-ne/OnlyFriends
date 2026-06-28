package com.ququ.activity.service.impl;

import com.ququ.activity.client.UserFeignClient;
import com.ququ.activity.service.UserClient;
import com.ququ.common.dto.UserBasicDTO;
import com.ququ.common.exception.BizException;
import com.ququ.common.response.Result;
import com.ququ.common.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

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
            throw new BizException(ResultCode.INTERNAL_ERROR.getCode(), "无法校验创建者状态，请确认用户服务已启动");
        }
    }

    @Override
    public int getUserCredit(Long userId) {
        try {
            Result<Integer> result = userFeignClient.getUserCredit(userId);
            if (success(result) && result.getData() != null) {
                return result.getData();
            }
            throw new BizException(ResultCode.INTERNAL_ERROR.getCode(), "无法获取用户信誉分");
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ResultCode.INTERNAL_ERROR.getCode(), "无法获取用户信誉分，请确认用户服务已启动");
        }
    }

    @Override
    public List<UserBasicDTO> getUsersByIds(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        try {
            Result<List<UserBasicDTO>> result = userFeignClient.getUsersByIds(userIds);
            return success(result) && result.getData() != null ? result.getData() : Collections.emptyList();
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private boolean success(Result<?> result) {
        return result != null && Integer.valueOf(200).equals(result.getCode());
    }
}
