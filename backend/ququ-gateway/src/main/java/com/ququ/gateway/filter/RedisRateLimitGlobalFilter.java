package com.ququ.gateway.filter;

import com.ququ.gateway.config.GatewayRateLimitProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RedisRateLimitGlobalFilter implements GlobalFilter, Ordered {
    private final GatewayRateLimitProperties properties;
    private final ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider;
    private final ConcurrentMap<String, AtomicInteger> localCounters = new ConcurrentHashMap<>();

    public RedisRateLimitGlobalFilter(GatewayRateLimitProperties properties,
                                      ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider) {
        this.properties = properties;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }
        String key = key(exchange);
        ReactiveStringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return localAllowed(key) ? chain.filter(exchange) : tooManyRequests(exchange);
        }
        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> redisTemplate.expire(key, Duration.ofSeconds(2)).thenReturn(count))
                .onErrorResume(ex -> Mono.just(localAllowed(key) ? 1L : (long) properties.getPermitsPerSecond() + 1))
                .flatMap(count -> count <= properties.getPermitsPerSecond()
                        ? chain.filter(exchange)
                        : tooManyRequests(exchange));
    }

    @Override
    public int getOrder() {
        return -110;
    }

    private boolean localAllowed(String key) {
        int count = localCounters.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
        localCounters.keySet().removeIf(existing -> !sameWindow(existing, key));
        return count <= properties.getPermitsPerSecond();
    }

    private boolean sameWindow(String left, String right) {
        int leftIndex = left.lastIndexOf(':');
        int rightIndex = right.lastIndexOf(':');
        return leftIndex > 0 && rightIndex > 0 && left.substring(leftIndex).equals(right.substring(rightIndex));
    }

    private String key(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (!StringUtils.hasText(userId)) {
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            userId = remoteAddress == null ? "anonymous" : remoteAddress.getAddress().getHostAddress();
        }
        return "gateway:rate:" + userId + ":" + Instant.now().getEpochSecond();
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }
}
