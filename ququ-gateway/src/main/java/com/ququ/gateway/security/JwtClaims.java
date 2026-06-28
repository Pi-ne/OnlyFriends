package com.ququ.gateway.security;

public record JwtClaims(
        Long userId,
        String userType,
        String userRole,
        String nickname
) {
}
