package com.onlyfriends.im.security;

import com.onlyfriends.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {
    public static final String PRINCIPAL_ATTR = "stompPrincipal";

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
            return false;
        }
        Claims claims = jwtUtil.parseToken(token);
        if (!"access".equals(claims.get("tokenType", String.class))) {
            return false;
        }
        attributes.put(PRINCIPAL_ATTR, new StompPrincipal(
                Long.valueOf(claims.getSubject()),
                claims.get("userType", Integer.class),
                claims.get("nickname", String.class)
        ));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private String resolveToken(ServerHttpRequest request) {
        List<String> authorization = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authorization != null && !authorization.isEmpty()) {
            String header = authorization.get(0);
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                return header.substring(7);
            }
        }
        return UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
    }
}
