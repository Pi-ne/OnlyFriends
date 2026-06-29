package com.onlyfriends.activity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyfriends.activity.entity.ActivityWaitlist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityRedisSupport {
    private static final String WAITLIST_KEY_PREFIX = "activity:waitlist:";
    private static final String WAITLIST_LOCK_PREFIX = "activity:waitlist:lock:";
    private static final String WAITLIST_PENDING_CHANNEL = "activity.waitlist.pending";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;

    @Value("${app.redis.enabled:false}")
    private boolean redisEnabled;

    public void addWaitlist(ActivityWaitlist waitlist) {
        StringRedisTemplate redisTemplate = redisTemplate();
        if (redisTemplate == null || waitlist == null || waitlist.getActivityId() == null || waitlist.getUserId() == null) {
            return;
        }
        try {
            double score = waitlist.getQueueNo() == null ? System.currentTimeMillis() : waitlist.getQueueNo();
            redisTemplate.opsForZSet().add(waitlistKey(waitlist.getActivityId()), String.valueOf(waitlist.getUserId()), score);
        } catch (RuntimeException ex) {
            log.warn("Failed to add activity waitlist to Redis, activityId={}, userId={}",
                    waitlist.getActivityId(), waitlist.getUserId(), ex);
        }
    }

    public void removeWaitlist(Long activityId, Long userId) {
        StringRedisTemplate redisTemplate = redisTemplate();
        if (redisTemplate == null || activityId == null || userId == null) {
            return;
        }
        try {
            redisTemplate.opsForZSet().remove(waitlistKey(activityId), String.valueOf(userId));
        } catch (RuntimeException ex) {
            log.warn("Failed to remove activity waitlist from Redis, activityId={}, userId={}", activityId, userId, ex);
        }
    }

    public void withPromotionLock(Long activityId, Runnable action) {
        StringRedisTemplate redisTemplate = redisTemplate();
        if (redisTemplate == null || activityId == null) {
            action.run();
            return;
        }
        String key = lockKey(activityId);
        String token = UUID.randomUUID().toString();
        boolean locked = false;
        try {
            locked = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, token, Duration.ofSeconds(10)));
            if (locked) {
                action.run();
            } else {
                log.info("Skip waitlist promotion because lock is held, activityId={}", activityId);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to use Redis waitlist lock, fallback to local execution, activityId={}", activityId, ex);
            action.run();
        } finally {
            if (locked) {
                releaseLock(redisTemplate, key, token);
            }
        }
    }

    public void publishWaitlistPending(ActivityWaitlist waitlist) {
        StringRedisTemplate redisTemplate = redisTemplate();
        if (redisTemplate == null || waitlist == null) {
            return;
        }
        try {
            redisTemplate.convertAndSend(WAITLIST_PENDING_CHANNEL, waitlistPayload(waitlist));
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Failed to publish waitlist pending event, activityId={}, userId={}",
                    waitlist.getActivityId(), waitlist.getUserId(), ex);
        }
    }

    private StringRedisTemplate redisTemplate() {
        if (!redisEnabled) {
            return null;
        }
        return redisTemplateProvider.getIfAvailable();
    }

    private void releaseLock(StringRedisTemplate redisTemplate, String key, String token) {
        try {
            String current = redisTemplate.opsForValue().get(key);
            if (token.equals(current)) {
                redisTemplate.delete(key);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to release Redis waitlist lock, key={}", key, ex);
        }
    }

    private String waitlistPayload(ActivityWaitlist waitlist) throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "WAITLIST_PENDING");
        payload.put("activityId", waitlist.getActivityId());
        payload.put("userId", waitlist.getUserId());
        payload.put("waitlistId", waitlist.getId());
        payload.put("queueNo", waitlist.getQueueNo());
        payload.put("pendingAt", waitlist.getPendingAt() == null ? null : waitlist.getPendingAt().toString());
        return objectMapper.writeValueAsString(payload);
    }

    private String waitlistKey(Long activityId) {
        return WAITLIST_KEY_PREFIX + activityId;
    }

    private String lockKey(Long activityId) {
        return WAITLIST_LOCK_PREFIX + activityId;
    }
}
