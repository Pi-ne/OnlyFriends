package com.onlyfriends.user.controller;

import com.onlyfriends.common.response.Result;
import com.onlyfriends.user.dto.request.LoginRequest;
import com.onlyfriends.user.dto.request.RefreshTokenRequest;
import com.onlyfriends.user.dto.request.RegisterRequest;
import com.onlyfriends.user.dto.request.WxLoginRequest;
import com.onlyfriends.user.dto.response.LoginResponse;
import com.onlyfriends.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public Result<Map<String, Long>> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = authService.register(request);
        return Result.success("注册成功，请查收激活邮件", Map.of("userId", userId));
    }

    @GetMapping("/activate")
    public Result<Void> activate(@RequestParam String token) {
        authService.activateAccount(token);
        return Result.success("账号激活成功", null);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/wx-login")
    public Result<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return Result.success(authService.wxLogin(request));
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authService.refreshToken(request.getRefreshToken()));
    }
}
