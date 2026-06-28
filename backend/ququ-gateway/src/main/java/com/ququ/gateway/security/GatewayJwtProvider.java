package com.ququ.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class GatewayJwtProvider {
    private final SecretKey secretKey;

    public GatewayJwtProvider(@Value("${jwt.secret}") String secret) {
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("jwt.secret must be configured and at least 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public JwtClaims validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!"access".equals(claims.get("tokenType", String.class))) {
                throw new JwtException("Only access token is accepted");
            }
            Long userId = Long.valueOf(claims.getSubject());
            Integer userType = claims.get("userType", Integer.class);
            String role = claims.get("role", String.class);
            if (!StringUtils.hasText(role)) {
                role = toDefaultRole(userType);
            }
            return new JwtClaims(
                    userId,
                    userType == null ? "" : String.valueOf(userType),
                    role,
                    claims.get("nickname", String.class)
            );
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtException("Invalid JWT token", ex);
        }
    }

    private String toDefaultRole(Integer userType) {
        if (userType == null) {
            return "";
        }
        return switch (userType) {
            case 1 -> "MERCHANT";
            case 9 -> "ADMIN";
            default -> "USER";
        };
    }
}
