package com.ququ.im.client;

import com.ququ.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "social-service", contextId = "imSocialFeignClient", url = "${social-service.base-url:http://localhost:8083}")
public interface SocialFeignClient {
    @GetMapping("/internal/social/friends/check")
    Result<Boolean> areFriends(@RequestParam("userIdA") Long userIdA, @RequestParam("userIdB") Long userIdB);

    @GetMapping("/internal/social/teams/{teamId}/members/check")
    Result<Boolean> isTeamMember(@PathVariable("teamId") Long teamId, @RequestParam("userId") Long userId);

    @GetMapping("/internal/social/teams/{teamId}/member-ids")
    Result<List<Long>> getTeamMemberIds(@PathVariable("teamId") Long teamId);
}
