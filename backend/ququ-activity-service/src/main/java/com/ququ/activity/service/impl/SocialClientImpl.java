package com.ququ.activity.service.impl;

import com.ququ.activity.client.SocialFeignClient;
import com.ququ.activity.service.SocialClient;
import com.ququ.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SocialClientImpl implements SocialClient {
    private final SocialFeignClient socialFeignClient;

    @Override
    public boolean isTeamMember(Long teamId, Long userId) {
        if (teamId == null || userId == null) {
            return false;
        }
        try {
            Result<Boolean> result = socialFeignClient.isTeamMember(teamId, userId);
            return result != null && Integer.valueOf(200).equals(result.getCode()) && Boolean.TRUE.equals(result.getData());
        } catch (Exception ex) {
            return false;
        }
    }
}
