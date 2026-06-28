package com.ququ.admin.controller;

import com.ququ.admin.dto.request.BanUserRequest;
import com.ququ.admin.dto.request.DisableTeamRequest;
import com.ququ.admin.dto.request.OfflineActivityRequest;
import com.ququ.admin.dto.request.ReviewRequest;
import com.ququ.admin.security.CurrentAdmin;
import com.ququ.admin.service.AdminService;
import com.ququ.common.exception.BizException;
import com.ququ.common.response.Result;
import com.ququ.common.response.ResultCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminManagementController {
    private final AdminService adminService;

    @GetMapping("/users")
    public Result<Object> users(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "20") Integer size,
                                @RequestParam(required = false) Integer userType,
                                @RequestParam(required = false) Integer status,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String nickname,
                                @RequestParam(required = false) String keyword) {
        return Result.success(adminService.users(page, size, userType, status, email, nickname, keyword));
    }

    @GetMapping("/users/{id}")
    public Result<Object> userDetail(@PathVariable Long id) {
        return Result.success(adminService.userDetail(id));
    }

    @PostMapping("/users/{id}/ban")
    public Result<Void> banUser(@AuthenticationPrincipal CurrentAdmin admin,
                                @PathVariable Long id,
                                @Valid @RequestBody BanUserRequest request) {
        adminService.banUser(requireAdmin(admin).getAdminId(), id, request);
        return Result.success();
    }

    @PostMapping("/users/{id}/unban")
    public Result<Void> unbanUser(@AuthenticationPrincipal CurrentAdmin admin, @PathVariable Long id) {
        adminService.unbanUser(requireAdmin(admin).getAdminId(), id);
        return Result.success();
    }

    @GetMapping("/merchant-applies")
    public Result<Object> merchantApplies(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "20") Integer size,
                                          @RequestParam(required = false) Integer status) {
        return Result.success(adminService.merchantApplies(page, size, status));
    }

    @GetMapping("/merchant-applies/{id}")
    public Result<Object> merchantApplyDetail(@PathVariable Long id) {
        return Result.success(adminService.merchantApplyDetail(id));
    }

    @PutMapping("/merchant-applies/{id}/review")
    public Result<Void> reviewMerchantApply(@AuthenticationPrincipal CurrentAdmin admin,
                                            @PathVariable Long id,
                                            @Valid @RequestBody ReviewRequest request) {
        adminService.reviewMerchantApply(requireAdmin(admin).getAdminId(), id, request);
        return Result.success();
    }

    @GetMapping("/activities/pending")
    public Result<Object> pendingActivities(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(adminService.pendingActivities(page, size));
    }

    @GetMapping("/activities")
    public Result<Object> activities(@RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "20") Integer size,
                                     @RequestParam(required = false) Integer status,
                                     @RequestParam(required = false) String keyword) {
        return Result.success(adminService.activities(page, size, status, keyword));
    }

    @GetMapping("/activities/{id}")
    public Result<Object> activityDetail(@PathVariable Long id) {
        return Result.success(adminService.activityDetail(id));
    }

    @PutMapping("/activities/{id}/review")
    public Result<Void> reviewActivity(@AuthenticationPrincipal CurrentAdmin admin,
                                       @PathVariable Long id,
                                       @Valid @RequestBody ReviewRequest request) {
        adminService.reviewActivity(requireAdmin(admin).getAdminId(), id, request);
        return Result.success();
    }

    @PostMapping("/activities/{id}/offline")
    public Result<Void> offlineActivity(@AuthenticationPrincipal CurrentAdmin admin,
                                        @PathVariable Long id,
                                        @Valid @RequestBody OfflineActivityRequest request) {
        adminService.offlineActivity(requireAdmin(admin).getAdminId(), id, request);
        return Result.success();
    }

    @PostMapping("/activities/{id}/restore")
    public Result<Void> restoreActivity(@AuthenticationPrincipal CurrentAdmin admin, @PathVariable Long id) {
        adminService.restoreActivity(requireAdmin(admin).getAdminId(), id);
        return Result.success();
    }

    @GetMapping("/teams")
    public Result<Object> teams(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "20") Integer size,
                                @RequestParam(required = false) Integer status,
                                @RequestParam(required = false) String keyword) {
        return Result.success(adminService.teams(page, size, status, keyword));
    }

    @GetMapping("/teams/{id}")
    public Result<Object> teamDetail(@PathVariable Long id) {
        return Result.success(adminService.teamDetail(id));
    }

    @GetMapping("/teams/{id}/members")
    public Result<Object> teamMembers(@PathVariable Long id) {
        return Result.success(adminService.teamMembers(id));
    }

    @PostMapping("/teams/{id}/disable")
    public Result<Void> disableTeam(@AuthenticationPrincipal CurrentAdmin admin,
                                    @PathVariable Long id,
                                    @Valid @RequestBody DisableTeamRequest request) {
        adminService.disableTeam(requireAdmin(admin).getAdminId(), id, request);
        return Result.success();
    }

    @PostMapping("/teams/{id}/restore")
    public Result<Void> restoreTeam(@AuthenticationPrincipal CurrentAdmin admin, @PathVariable Long id) {
        adminService.restoreTeam(requireAdmin(admin).getAdminId(), id);
        return Result.success();
    }

    private CurrentAdmin requireAdmin(CurrentAdmin admin) {
        if (admin == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return admin;
    }
}
