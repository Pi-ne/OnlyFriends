package com.ququ.im.security;

import java.security.Principal;

public class StompPrincipal implements Principal {
    private final Long userId;
    private final Integer userType;
    private final String nickname;

    public StompPrincipal(Long userId, Integer userType, String nickname) {
        this.userId = userId;
        this.userType = userType;
        this.nickname = nickname;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getUserType() {
        return userType;
    }

    public String getNickname() {
        return nickname;
    }
}
