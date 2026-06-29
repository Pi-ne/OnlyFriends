package com.onlyfriends.gateway;

import com.onlyfriends.gateway.config.GatewayRateLimitProperties;
import com.onlyfriends.gateway.filter.RedisRateLimitGlobalFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisRateLimitGlobalFilterTest {
    @Test
    void localFallbackRejectsWhenLimitExceeded() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setEnabled(true);
        properties.setPermitsPerSecond(1);
        RedisRateLimitGlobalFilter filter = new RedisRateLimitGlobalFilter(properties, mock(ObjectProvider.class));

        MockServerWebExchange first = exchange();
        MockServerWebExchange second = exchange();

        filter.filter(first, ex -> Mono.empty()).block();
        filter.filter(second, ex -> Mono.empty()).block();

        assertThat(first.getResponse().getStatusCode()).isNull();
        assertThat(second.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/activities")
                .header("X-User-Id", "10001"));
    }
}
