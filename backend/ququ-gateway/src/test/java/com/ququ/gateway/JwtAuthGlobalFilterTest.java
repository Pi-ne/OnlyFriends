package com.ququ.gateway;

import com.ququ.gateway.filter.JwtAuthGlobalFilter;
import com.ququ.gateway.security.GatewayJwtProvider;
import com.ququ.gateway.security.JwtClaims;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthGlobalFilterTest {
    @Test
    void blocksInternalPathBeforeJwtValidation() {
        GatewayJwtProvider jwtProvider = mock(GatewayJwtProvider.class);
        JwtAuthGlobalFilter filter = new JwtAuthGlobalFilter(jwtProvider);
        MockServerWebExchange exchange = exchange("/internal/users/10001/valid");

        filter.filter(exchange, passthroughChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(jwtProvider, never()).validateAccessToken("token");
    }

    @Test
    void rejectsNonAdminRoleForAdminApi() {
        GatewayJwtProvider jwtProvider = mock(GatewayJwtProvider.class);
        when(jwtProvider.validateAccessToken("token")).thenReturn(new JwtClaims(10001L, "0", "USER", "普通用户"));
        JwtAuthGlobalFilter filter = new JwtAuthGlobalFilter(jwtProvider);
        MockServerWebExchange exchange = exchange("/api/v1/admin/users");

        filter.filter(exchange, passthroughChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void allowsAdminRoleForAdminApiAndAddsUserHeaders() {
        GatewayJwtProvider jwtProvider = mock(GatewayJwtProvider.class);
        when(jwtProvider.validateAccessToken("token")).thenReturn(new JwtClaims(90001L, "9", "ADMIN", "管理员"));
        JwtAuthGlobalFilter filter = new JwtAuthGlobalFilter(jwtProvider);
        MockServerWebExchange exchange = exchange("/api/v1/admin/users");
        AtomicReference<ServerWebExchange> passedExchange = new AtomicReference<>();

        filter.filter(exchange, next -> {
            passedExchange.set(next);
            return Mono.empty();
        }).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(passedExchange.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("90001");
        assertThat(passedExchange.get().getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("ADMIN");
    }

    private MockServerWebExchange exchange(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer token"));
    }

    private GatewayFilterChain passthroughChain() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        return exchange -> {
            invoked.set(true);
            return Mono.empty();
        };
    }
}
