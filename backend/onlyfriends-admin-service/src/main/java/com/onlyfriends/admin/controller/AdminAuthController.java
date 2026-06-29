package com.onlyfriends.admin.controller;

import com.onlyfriends.admin.dto.request.AdminLoginRequest;
import com.onlyfriends.admin.dto.request.ChangePasswordRequest;
import com.onlyfriends.admin.dto.response.AdminLoginResponse;
import com.onlyfriends.admin.security.CurrentAdmin;
import com.onlyfriends.admin.service.AdminService;
import com.onlyfriends.common.exception.BizException;
import com.onlyfriends.common.response.Result;
import com.onlyfriends.common.response.ResultCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {
    private final AdminService adminService;

    @PostMapping("/login")
    public Result<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return Result.success(adminService.login(request));
    }

    @PostMapping("/password")
    public Result<Void> changePassword(@AuthenticationPrincipal CurrentAdmin admin,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        if (admin == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        adminService.changePassword(admin.getAdminId(), request);
        return Result.success();
    }
}
