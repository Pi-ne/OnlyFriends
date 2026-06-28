package com.ququ.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ququ.activity.entity.ActivityWaitlist;
import com.ququ.activity.service.ActivityRedisSupport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityRedisSupportTest {
    @Test
    void addAndRemoveWaitlistUseRedisZSet() {
        RedisFixture fixture = redisFixture();
        ActivityRedisSupport support = support(fixture.redisTemplate);
        ActivityWaitlist waitlist = waitlist();

        support.addWaitlist(waitlist);
        support.removeWaitlist(1001L, 2002L);

        verify(fixture.zSetOperations).add("activity:waitlist:1001", "2002", 3D);
        verify(fixture.zSetOperations).remove("activity:waitlist:1001", "2002");
    }

    @Test
    void promotionLockExecutesActionAndReleasesOwnedLock() {
        RedisFixture fixture = redisFixture();
        ActivityRedisSupport support = support(fixture.redisTemplate);
        AtomicBoolean executed = new AtomicBoolean(false);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        when(fixture.valueOperations.setIfAbsent(eq("activity:waitlist:lock:1001"), tokenCaptor.capture(), any(Duration.class)))
                .thenReturn(true);
        when(fixture.valueOperations.get("activity:waitlist:lock:1001")).thenAnswer(invocation -> tokenCaptor.getValue());

        support.withPromotionLock(1001L, () -> executed.set(true));

        assertThat(executed).isTrue();
        verify(fixture.redisTemplate).delete("activity:waitlist:lock:1001");
    }

    @Test
    void publishWaitlistPendingSendsJsonMessage() {
        RedisFixture fixture = redisFixture();
        ActivityRedisSupport support = support(fixture.redisTemplate);

        support.publishWaitlistPending(waitlist());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(fixture.redisTemplate).convertAndSend(eq("activity.waitlist.pending"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("\"event\":\"WAITLIST_PENDING\"");
        assertThat(payloadCaptor.getValue()).contains("\"activityId\":1001");
        assertThat(payloadCaptor.getValue()).contains("\"userId\":2002");
    }

    private ActivityRedisSupport support(StringRedisTemplate redisTemplate) {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redisTemplate);
        ActivityRedisSupport support = new ActivityRedisSupport(provider, new ObjectMapper());
        ReflectionTestUtils.setField(support, "redisEnabled", true);
        return support;
    }

    private ActivityWaitlist waitlist() {
        ActivityWaitlist waitlist = new ActivityWaitlist();
        waitlist.setId(10L);
        waitlist.setActivityId(1001L);
        waitlist.setUserId(2002L);
        waitlist.setQueueNo(3);
        waitlist.setPendingAt(LocalDateTime.of(2026, 6, 27, 10, 0));
        return waitlist;
    }

    @SuppressWarnings("unchecked")
    private RedisFixture redisFixture() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.convertAndSend(anyString(), anyString())).thenReturn(1L);
        return new RedisFixture(redisTemplate, zSetOperations, valueOperations);
    }

    private record RedisFixture(StringRedisTemplate redisTemplate,
                                ZSetOperations<String, String> zSetOperations,
                                ValueOperations<String, String> valueOperations) {
    }
}
