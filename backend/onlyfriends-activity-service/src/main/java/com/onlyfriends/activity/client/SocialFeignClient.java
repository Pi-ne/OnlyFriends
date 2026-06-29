package com.onlyfriends.activity.client;

import com.onlyfriends.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "social-service", contextId = "activitySocialFeignClient", url = "${social-service.base-url:http://localhost:8083}")
public interface SocialFeignClient {
    @GetMapping("/internal/social/teams/{teamId}/members/check")
    Result<Boolean> isTeamMember(@PathVariable("teamId") Long teamId, @RequestParam("userId") Long userId);
}
