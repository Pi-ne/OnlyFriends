package com.onlyfriends.im;

import com.onlyfriends.common.util.JwtUtil;
import com.onlyfriends.im.security.StompPrincipal;
import com.onlyfriends.im.security.WebSocketAuthHandshakeInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketAuthHandshakeInterceptorTest {
    private final JwtUtil jwtUtil = new JwtUtil("test-test-test-test-test-test-32bytes", 7200000L, 604800000L);
    private final WebSocketAuthHandshakeInterceptor interceptor = new WebSocketAuthHandshakeInterceptor(jwtUtil);

    @Test
    void acceptsAccessTokenFromAuthorizationHeader() {
        String token = jwtUtil.generateAccessToken(10001L, 0, "tester");
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(
                request("/ws/im", "Bearer " + token),
                new ServletServerHttpResponse(new MockHttpServletResponse()),
                null,
                attributes
        );

        assertThat(accepted).isTrue();
        StompPrincipal principal = (StompPrincipal) attributes.get(WebSocketAuthHandshakeInterceptor.PRINCIPAL_ATTR);
        assertThat(principal.getUserId()).isEqualTo(10001L);
        assertThat(principal.getName()).isEqualTo("10001");
    }

    @Test
    void rejectsRefreshToken() {
        String token = jwtUtil.generateRefreshToken(10001L);

        boolean accepted = interceptor.beforeHandshake(
                request("/ws/im?token=" + token, null),
                new ServletServerHttpResponse(new MockHttpServletResponse()),
                null,
                new HashMap<>()
        );

        assertThat(accepted).isFalse();
    }

    private ServletServerHttpRequest request(String uri, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        if (authorization != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        return new ServletServerHttpRequest(request);
    }
}
