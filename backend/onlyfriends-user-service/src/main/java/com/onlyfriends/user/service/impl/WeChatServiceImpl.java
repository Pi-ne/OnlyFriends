package com.onlyfriends.user.service.impl;

import com.onlyfriends.common.exception.BizException;
import com.onlyfriends.common.response.ResultCode;
import com.onlyfriends.user.dto.wechat.WeChatSessionResponse;
import com.onlyfriends.user.service.WeChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatServiceImpl implements WeChatService {
    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final RestTemplate restTemplate;

    @Value("${app.wechat.app-id:}")
    private String appId;

    @Value("${app.wechat.app-secret:}")
    private String appSecret;

    @Value("${app.wechat.mock-enabled:false}")
    private boolean mockEnabled;

    @Override
    public WeChatSession exchangeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "微信登录 code 不能为空");
        }
        if (mockEnabled) {
            log.debug("WeChat mock login enabled for tests");
            return new WeChatSession("mock_" + code.trim(), null);
        }
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            log.error("WeChat login is not configured: app-id or app-secret is missing");
            throw new BizException(ResultCode.WECHAT_LOGIN_FAILED);
        }

        String url = UriComponentsBuilder.fromHttpUrl(CODE2SESSION_URL)
                .queryParam("appid", appId)
                .queryParam("secret", appSecret)
                .queryParam("js_code", code.trim())
                .queryParam("grant_type", "authorization_code")
                .toUriString();

        WeChatSessionResponse response = restTemplate.getForObject(url, WeChatSessionResponse.class);
        if (response == null) {
            throw new BizException(ResultCode.WECHAT_LOGIN_FAILED);
        }
        if (response.getErrcode() != null && response.getErrcode() != 0) {
            log.warn("WeChat code2session failed: errcode={}, errmsg={}", response.getErrcode(), response.getErrmsg());
            throw new BizException(ResultCode.WECHAT_LOGIN_FAILED);
        }
        if (!StringUtils.hasText(response.getOpenid())) {
            throw new BizException(ResultCode.WECHAT_LOGIN_FAILED);
        }
        return new WeChatSession(response.getOpenid(), response.getUnionid());
    }
}
