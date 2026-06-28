package com.ququ.im.service.impl;

import com.ququ.common.exception.BizException;
import com.ququ.common.response.Result;
import com.ququ.common.response.ResultCode;
import com.ququ.im.client.SocialFeignClient;
import com.ququ.im.service.SocialClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialClientImpl implements SocialClient {
    private final SocialFeignClient socialFeignClient;

    @Override
    public boolean areFriends(Long userIdA, Long userIdB) {
        try {
            Result<Boolean> result = socialFeignClient.areFriends(userIdA, userIdB);
            return success(result) && Boolean.TRUE.equals(result.getData());
        } catch (Exception ex) {
            throw new BizException(ResultCode.INTERNAL_ERROR.getCode(), "无法校验好友关系，请确认社群服务已启动");
        }
    }

    @Override
    public boolean isTeamMember(Long teamId, Long userId) {
        try {
            Result<Boolean> result = socialFeignClient.isTeamMember(teamId, userId);
            return success(result) && Boolean.TRUE.equals(result.getData());
        } catch (Exception ex) {
            throw new BizException(ResultCode.INTERNAL_ERROR.getCode(), "无法校验小队成员关系，请确认社群服务已启动");
        }
    }

    @Override
    public List<Long> getTeamMemberIds(Long teamId) {
        try {
            Result<List<Long>> result = socialFeignClient.getTeamMemberIds(teamId);
            return success(result) && result.getData() != null ? result.getData() : List.of();
        } catch (Exception ex) {
            throw new BizException(ResultCode.INTERNAL_ERROR.getCode(), "无法获取小队成员，请确认社群服务已启动");
        }
    }

    private boolean success(Result<?> result) {
        return result != null && Integer.valueOf(200).equals(result.getCode());
    }
}
