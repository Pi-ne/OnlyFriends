package com.ququ.user.service;

import com.ququ.user.dto.request.LoginRequest;
import com.ququ.user.dto.request.RegisterRequest;
import com.ququ.user.dto.response.LoginResponse;

public interface AuthService {
    Long register(RegisterRequest request);

    void activateAccount(String token);

    LoginResponse login(LoginRequest request);

    LoginResponse refreshToken(String refreshToken);
}
