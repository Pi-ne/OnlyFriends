package com.onlyfriends.user.service;

public interface RefreshTokenStore {
    void save(Long userId, String refreshToken, long expireMillis);

    boolean matches(Long userId, String refreshToken);
}
