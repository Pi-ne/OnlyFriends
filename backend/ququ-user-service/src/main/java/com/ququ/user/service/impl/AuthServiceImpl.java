package com.ququ.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ququ.common.exception.BizException;
import com.ququ.common.response.ResultCode;
import com.ququ.common.util.JwtUtil;
import com.ququ.user.dto.request.LoginRequest;
import com.ququ.user.dto.request.RegisterRequest;
import com.ququ.user.dto.response.LoginResponse;
import com.ququ.user.dto.response.UserInfoResponse;
import com.ququ.user.entity.User;
import com.ququ.user.mapper.UserMapper;
import com.ququ.user.service.AuthService;
import com.ququ.user.service.MailService;
import com.ququ.user.service.RefreshTokenStore;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final int STATUS_NOT_ACTIVATED = 0;
    private static final int STATUS_NORMAL = 1;
    private static final int STATUS_BANNED = 2;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MailService mailService;
    private final RefreshTokenStore refreshTokenStore;

    @Override
    @Transactional
    public Long register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String nickname = request.getNickname().trim();
        if (existsByEmail(email)) {
            throw new BizException(ResultCode.EMAIL_ALREADY_EXISTS);
        }
        if (existsByNickname(nickname)) {
            throw new BizException(ResultCode.NICKNAME_ALREADY_EXISTS);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(nickname);
        user.setGender(0);
        user.setUserType(0);
        user.setStatus(STATUS_NOT_ACTIVATED);
        user.setCreditScore(100);
        user.setActivateToken(UUID.randomUUID().toString().replace("-", ""));
        user.setDeleted(0);
        userMapper.insert(user);
        mailService.sendActivationMail(email, nickname, user.getActivateToken());
        return user.getId();
    }

    @Override
    @Transactional
    public void activateAccount(String token) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getActivateToken, token));
        if (user == null) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        user.setStatus(STATUS_NORMAL);
        user.setActivateToken(null);
        userMapper.updateById(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail().trim().toLowerCase()));
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(ResultCode.WRONG_PASSWORD);
        }
        ensureLoginAllowed(user);
        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        Claims claims;
        try {
            claims = jwtUtil.parseToken(refreshToken);
        } catch (Exception ex) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        if (!"refresh".equals(claims.get("tokenType", String.class))) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        Long userId = Long.valueOf(claims.getSubject());
        if (!refreshTokenStore.matches(userId, refreshToken)) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        ensureLoginAllowed(user);
        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUserType(), user.getNickname());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        refreshTokenStore.save(user.getId(), refreshToken, jwtUtil.getRefreshTokenExpire());
        UserInfoResponse userInfo = new UserInfoResponse(user.getId(), user.getNickname(), user.getAvatarUrl(), user.getUserType(), user.getCreditScore());
        return new LoginResponse(accessToken, refreshToken, Instant.now().toEpochMilli() + jwtUtil.getAccessTokenExpire(), userInfo);
    }

    private void ensureLoginAllowed(User user) {
        if (Integer.valueOf(STATUS_NOT_ACTIVATED).equals(user.getStatus())) {
            throw new BizException(ResultCode.USER_NOT_ACTIVATED);
        }
        if (Integer.valueOf(STATUS_BANNED).equals(user.getStatus())) {
            throw new BizException(ResultCode.USER_BANNED);
        }
    }

    private boolean existsByEmail(String email) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0;
    }

    private boolean existsByNickname(String nickname) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getNickname, nickname)) > 0;
    }
}
