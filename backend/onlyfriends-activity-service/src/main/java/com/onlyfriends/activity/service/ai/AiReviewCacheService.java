package com.onlyfriends.activity.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyfriends.activity.dto.ai.AiReviewRequest;
import com.onlyfriends.activity.dto.ai.AiReviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReviewCacheService {
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, AiReviewResponse> localCache = new ConcurrentHashMap<>();

    @Value("${app.redis.enabled:false}")
    private boolean redisEnabled;

    @Value("${app.ai.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${app.ai.cache.ttl-hours:24}")
    private long ttlHours;

    public AiReviewResponse get(AiReviewRequest request) {
        if (!cacheEnabled) {
            return null;
        }
        String key = key(request);
        AiReviewResponse local = localCache.get(key);
        if (local != null) {
            return local;
        }
        if (!redisEnabled) {
            return null;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return null;
        }
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            AiReviewResponse response = objectMapper.readValue(value, AiReviewResponse.class);
            localCache.put(key, response);
            return response;
        } catch (Exception ex) {
            log.warn("Failed to read AI review cache", ex);
            return null;
        }
    }

    public void put(AiReviewRequest request, AiReviewResponse response) {
        if (!cacheEnabled || response == null) {
            return;
        }
        String key = key(request);
        localCache.put(key, response);
        if (!redisEnabled) {
            return;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(response), Duration.ofHours(ttlHours));
        } catch (Exception ex) {
            log.warn("Failed to write AI review cache", ex);
        }
    }

    private String key(AiReviewRequest request) {
        String content = normalize(request);
        return "ai:review:" + sha256(content);
    }

    private String normalize(AiReviewRequest request) {
        List<String> tags = request == null || request.getTags() == null
                ? List.of()
                : request.getTags().stream().sorted(Comparator.naturalOrder()).toList();
        return String.join("|",
                safe(request == null ? null : request.getTitle()),
                safe(request == null ? null : request.getDescription()),
                String.join(",", tags),
                String.valueOf(request == null ? null : request.getMaxParticipants()));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }
}
