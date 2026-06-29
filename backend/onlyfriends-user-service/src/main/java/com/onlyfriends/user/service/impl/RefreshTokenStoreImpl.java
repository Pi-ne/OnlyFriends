package com.onlyfriends.user.service.impl;

import com.onlyfriends.user.service.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenStoreImpl implements RefreshTokenStore {
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Value("${app.redis.enabled:false}")
    private boolean redisEnabled;

    @Override
    public void save(Long userId, String refreshToken, long expireMillis) {
        if (!redisEnabled) {
            return;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(userId), refreshToken, Duration.ofMillis(expireMillis));
        } catch (Exception ex) {
            log.warn("Failed to save refresh token to Redis for user {}", userId, ex);
        }
    }

    @Override
    public boolean matches(Long userId, String refreshToken) {
        if (!redisEnabled) {
            return true;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return true;
        }
        try {
            String stored = redisTemplate.opsForValue().get(key(userId));
            return refreshToken.equals(stored);
        } catch (Exception ex) {
            log.warn("Failed to validate refresh token from Redis for user {}", userId, ex);
            return true;
        }
    }

    private String key(Long userId) {
        return "user:token:refresh:" + userId;
    }
}
