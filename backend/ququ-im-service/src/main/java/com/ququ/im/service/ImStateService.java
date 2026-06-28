package com.ququ.im.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class ImStateService {
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final boolean redisEnabled;
    private final Duration onlineTtl;
    private final ConcurrentMap<Long, Set<String>> localOnlineSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> localUnread = new ConcurrentHashMap<>();

    public ImStateService(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                          @Value("${app.redis.enabled:false}") boolean redisEnabled,
                          @Value("${app.im.online-ttl-seconds:60}") long onlineTtlSeconds) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.redisEnabled = redisEnabled;
        this.onlineTtl = Duration.ofSeconds(onlineTtlSeconds);
    }

    public void markOnline(Long userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }
        localOnlineSessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
        redis(redis -> redis.opsForValue().set(onlineKey(userId), sessionId, onlineTtl));
    }

    public void refreshOnline(Long userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }
        localOnlineSessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
        redis(redis -> redis.expire(onlineKey(userId), onlineTtl));
    }

    public void markOffline(Long userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }
        Set<String> sessions = localOnlineSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                localOnlineSessions.remove(userId);
            }
        }
        redis(redis -> {
            String key = onlineKey(userId);
            String stored = redis.opsForValue().get(key);
            if (sessionId.equals(stored)) {
                redis.delete(key);
            }
        });
    }

    public boolean isOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        if (localOnlineSessions.containsKey(userId)) {
            return true;
        }
        String value = redisValue(onlineKey(userId));
        return value != null;
    }

    public void incrementUnread(Long convId, Collection<Long> userIds, Long senderId) {
        if (convId == null || userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds) {
            if (userId == null || userId.equals(senderId)) {
                continue;
            }
            localUnread.merge(unreadKey(convId, userId), 1L, Long::sum);
            redis(redis -> redis.opsForValue().increment(unreadKey(convId, userId)));
        }
    }

    public void clearUnread(Long convId, Long userId) {
        if (convId == null || userId == null) {
            return;
        }
        localUnread.remove(unreadKey(convId, userId));
        redis(redis -> redis.delete(unreadKey(convId, userId)));
    }

    public Long getUnread(Long convId, Long userId, java.util.function.LongSupplier fallback) {
        String key = unreadKey(convId, userId);
        Long local = localUnread.get(key);
        if (local != null) {
            return local;
        }
        String value = redisValue(key);
        if (value != null) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException ex) {
                log.warn("Invalid Redis unread value for {}", key, ex);
            }
        }
        return fallback.getAsLong();
    }

    private String redisValue(String key) {
        if (!redisEnabled) {
            return null;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception ex) {
            log.warn("Failed to read Redis key {}", key, ex);
            return null;
        }
    }

    private void redis(RedisAction action) {
        if (!redisEnabled) {
            return;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        try {
            action.accept(redisTemplate);
        } catch (Exception ex) {
            log.warn("Failed to update IM Redis state", ex);
        }
    }

    private String onlineKey(Long userId) {
        return "user:ws:online:" + userId;
    }

    private String unreadKey(Long convId, Long userId) {
        return "im:unread:" + convId + ":" + userId;
    }

    @FunctionalInterface
    private interface RedisAction {
        void accept(StringRedisTemplate redisTemplate);
    }
}
