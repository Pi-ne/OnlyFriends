package com.onlyfriends.gateway.security;

public record JwtClaims(
        Long userId,
        String userType,
        String userRole,
        String nickname
) {
}
