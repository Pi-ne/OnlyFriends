package com.onlyfriends.user.service;

public interface WeChatService {
    WeChatSession exchangeCode(String code);

    record WeChatSession(String openid, String unionid) {
    }
}
