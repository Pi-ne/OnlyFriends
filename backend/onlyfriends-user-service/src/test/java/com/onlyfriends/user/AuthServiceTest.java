package com.onlyfriends.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.onlyfriends.common.exception.BizException;
import com.onlyfriends.user.dto.request.LoginRequest;
import com.onlyfriends.user.dto.request.RegisterRequest;
import com.onlyfriends.user.dto.response.LoginResponse;
import com.onlyfriends.user.entity.User;
import com.onlyfriends.user.mapper.UserMapper;
import com.onlyfriends.user.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
class AuthServiceTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Test
    void registerActivateAndLogin() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("user@example.com");
        registerRequest.setPassword("Abc123456");
        registerRequest.setNickname("OnlyFriends达人");

        Long userId = authService.register(registerRequest);
        User created = userMapper.selectById(userId);

        assertThat(created.getStatus()).isZero();
        assertThat(created.getActivateToken()).isNotBlank();

        LoginRequest loginBeforeActivate = new LoginRequest();
        loginBeforeActivate.setEmail("user@example.com");
        loginBeforeActivate.setPassword("Abc123456");
        assertThatThrownBy(() -> authService.login(loginBeforeActivate)).isInstanceOf(BizException.class);

        authService.activateAccount(created.getActivateToken());
        User activated = userMapper.selectById(userId);
        assertThat(activated.getStatus()).isEqualTo(1);
        assertThat(activated.getActivateToken()).isNull();

        LoginResponse loginResponse = authService.login(loginBeforeActivate);
        assertThat(loginResponse.getAccessToken()).isNotBlank();
        assertThat(loginResponse.getRefreshToken()).isNotBlank();
        assertThat(loginResponse.getUserInfo().getUserId()).isEqualTo(userId);
    }

    @Test
    void duplicateEmailIsRejected() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("duplicate@example.com");
        request.setPassword("Abc123456");
        request.setNickname("重复昵称1");
        authService.register(request);

        RegisterRequest duplicate = new RegisterRequest();
        duplicate.setEmail("duplicate@example.com");
        duplicate.setPassword("Abc123456");
        duplicate.setNickname("重复昵称2");

        assertThatThrownBy(() -> authService.register(duplicate)).isInstanceOf(BizException.class);
        assertThat(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, "duplicate@example.com"))).isEqualTo(1);
    }
}
