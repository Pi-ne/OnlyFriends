package com.onlyfriends.gateway.filter;

import com.onlyfriends.gateway.security.GatewayJwtProvider;
import com.onlyfriends.gateway.security.JwtClaims;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> WHITE_LIST = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/activate",
            "/api/v1/auth/refresh",
            "/api/v1/admin/auth/login",
            "/v3/api-docs/**",
            "/*/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/doc.html",
            "/favicon.ico",
            "/actuator/health"
    );
    private static final List<String> GET_WHITE_LIST = List.of(
            "/api/v1/activities",
            "/api/v1/activities/*",
            "/api/v1/activities/templates",
            "/api/v1/activities/tags"
    );

    private final GatewayJwtProvider jwtProvider;

    public JwtAuthGlobalFilter(GatewayJwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().pathWithinApplication().value();
        if (isPreflight(request)) {
            return chain.filter(exchange);
        }
        if (isInternalPath(path)) {
            return forbidden(exchange, "Internal API is not exposed through gateway");
        }
        if (isWhitelisted(path) || isGetWhitelisted(request, path)) {
            return chain.filter(exchange);
        }

        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, "Missing Authorization Bearer token");
        }

        try {
            JwtClaims claims = jwtProvider.validateAccessToken(token);
            if (isAdminPath(path) && !isAdminRole(claims.userRole())) {
                return forbidden(exchange, "Admin role is required");
            }
            ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(headers -> {
                        headers.remove("X-User-Id");
                        headers.remove("X-User-Type");
                        headers.remove("X-User-Role");
                        headers.remove("X-Nickname");
                    })
                    .header("X-User-Id", String.valueOf(claims.userId()))
                    .header("X-User-Type", claims.userType())
                    .header("X-User-Role", claims.userRole())
                    .headers(headers -> addNickname(headers, claims.nickname()))
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (JwtException ex) {
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPreflight(ServerHttpRequest request) {
        return request.getMethod() == HttpMethod.OPTIONS;
    }

    private boolean isWhitelisted(String path) {
        return WHITE_LIST.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private boolean isGetWhitelisted(ServerHttpRequest request, String path) {
        return request.getMethod() == HttpMethod.GET
                && GET_WHITE_LIST.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private boolean isInternalPath(String path) {
        return path.equals("/internal") || path.startsWith("/internal/") || path.contains("/internal/");
    }

    private boolean isAdminPath(String path) {
        return PATH_MATCHER.match("/api/v1/admin/**", path) && !PATH_MATCHER.match("/api/v1/admin/auth/login", path);
    }

    private boolean isAdminRole(String role) {
        return "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
    }

    private String resolveToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        String queryToken = request.getQueryParams().getFirst("token");
        return StringUtils.hasText(queryToken) ? queryToken : null;
    }

    private void addNickname(HttpHeaders headers, String nickname) {
        if (StringUtils.hasText(nickname)) {
            headers.add("X-Nickname", nickname);
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        return error(exchange, HttpStatus.UNAUTHORIZED, 401, message);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        return error(exchange, HttpStatus.FORBIDDEN, 403, message);
    }

    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, int code, String message) {
        byte[] body = ("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}")
                .replace("\"code\":401", "\"code\":" + code)
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setContentLength(body.length);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}
