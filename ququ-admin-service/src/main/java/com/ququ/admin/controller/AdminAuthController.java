package com.ququ.admin.controller;

import com.ququ.admin.dto.request.AdminLoginRequest;
import com.ququ.admin.dto.request.ChangePasswordRequest;
import com.ququ.admin.dto.response.AdminLoginResponse;
import com.ququ.admin.security.CurrentAdmin;
import com.ququ.admin.service.AdminService;
import com.ququ.common.exception.BizException;
import com.ququ.common.response.Result;
import com.ququ.common.response.ResultCode;
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
