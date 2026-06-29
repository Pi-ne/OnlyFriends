package com.onlyfriends.user.service;

import com.onlyfriends.user.dto.request.LoginRequest;
import com.onlyfriends.user.dto.request.RegisterRequest;
import com.onlyfriends.user.dto.response.LoginResponse;

public interface AuthService {
    Long register(RegisterRequest request);

    void activateAccount(String token);

    LoginResponse login(LoginRequest request);

    LoginResponse refreshToken(String refreshToken);
}
