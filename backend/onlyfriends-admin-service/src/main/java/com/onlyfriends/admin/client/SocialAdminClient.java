package com.onlyfriends.admin.client;

import com.onlyfriends.admin.dto.request.DisableTeamRequest;
import com.onlyfriends.admin.dto.request.AdminRestoreTeamRequest;
import com.onlyfriends.common.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "social-service", contextId = "adminSocialClient", url = "${social-service.base-url:http://localhost:8083}")
public interface SocialAdminClient {
    @GetMapping("/internal/admin/teams")
    Result<Object> teams(@RequestParam("page") Integer page,
                         @RequestParam("size") Integer size,
                         @RequestParam(value = "status", required = false) Integer status,
                         @RequestParam(value = "keyword", required = false) String keyword);

    @GetMapping("/internal/admin/teams/{id}")
    Result<Object> teamDetail(@PathVariable("id") Long teamId);

    @GetMapping("/internal/admin/teams/{id}/members")
    Result<Object> teamMembers(@PathVariable("id") Long teamId);

    @PostMapping("/internal/admin/teams/{id}/disable")
    Result<Void> disableTeam(@RequestParam("adminId") Long adminId,
                             @PathVariable("id") Long teamId,
                             @RequestBody DisableTeamRequest request);

    @PostMapping("/internal/admin/teams/{id}/restore")
    Result<Void> restoreTeam(@PathVariable("id") Long teamId, @RequestBody AdminRestoreTeamRequest request);
}
