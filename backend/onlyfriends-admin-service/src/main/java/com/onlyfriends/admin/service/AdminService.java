package com.onlyfriends.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.onlyfriends.admin.client.ActivityAdminClient;
import com.onlyfriends.admin.client.SocialAdminClient;
import com.onlyfriends.admin.client.UserAdminClient;
import com.onlyfriends.admin.dto.request.AdminLoginRequest;
import com.onlyfriends.admin.dto.request.AdminRestoreTeamRequest;
import com.onlyfriends.admin.dto.request.BanUserRequest;
import com.onlyfriends.admin.dto.request.ChangePasswordRequest;
import com.onlyfriends.admin.dto.request.DisableTeamRequest;
import com.onlyfriends.admin.dto.request.OfflineActivityRequest;
import com.onlyfriends.admin.dto.request.ReviewRequest;
import com.onlyfriends.admin.dto.response.AdminLoginResponse;
import com.onlyfriends.admin.entity.AdminOperationLog;
import com.onlyfriends.admin.entity.AdminUser;
import com.onlyfriends.admin.mapper.AdminOperationLogMapper;
import com.onlyfriends.admin.mapper.AdminUserMapper;
import com.onlyfriends.common.exception.BizException;
import com.onlyfriends.common.response.ResultCode;
import com.onlyfriends.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminService {
    private static final int ADMIN_ENABLED = 1;

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserAdminClient userAdminClient;
    private final ActivityAdminClient activityAdminClient;
    private final SocialAdminClient socialAdminClient;
    private final AdminOperationLogMapper operationLogMapper;

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminUser admin = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, request.getUsername()));
        if (admin == null || !passwordMatches(request.getPassword(), admin.getPasswordHash())) {
            throw new BizException(ResultCode.UNAUTHORIZED.getCode(), "管理员账号或密码错误");
        }
        if (!Integer.valueOf(ADMIN_ENABLED).equals(admin.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "管理员账号已停用");
        }
        admin.setLastLoginAt(LocalDateTime.now());
        adminUserMapper.updateById(admin);
        String token = jwtUtil.generateAccessToken(admin.getId(), 9, admin.getNickname());
        return new AdminLoginResponse(token, jwtUtil.getAccessTokenExpire(), admin.getId(), admin.getUsername(), admin.getNickname());
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword != null && storedPassword.startsWith("{noop}")) {
            return storedPassword.substring("{noop}".length()).equals(rawPassword);
        }
        return passwordEncoder.matches(rawPassword, storedPassword);
    }

    @Transactional
    public void changePassword(Long adminId, ChangePasswordRequest request) {
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        if (!passwordMatches(request.getOldPassword(), admin.getPasswordHash())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "旧密码不正确");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "新密码不能与旧密码相同");
        }
        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        adminUserMapper.updateById(admin);
        logOperation(adminId, "CHANGE_PASSWORD", "ADMIN", adminId, "管理员修改自己的登录密码");
    }

    public Object users(Integer page, Integer size, Integer userType, Integer status, String email, String nickname, String keyword) {
        return data(userAdminClient.users(page, size, userType, status, email, nickname, keyword), "用户服务调用失败");
    }

    public Object userDetail(Long userId) {
        return data(userAdminClient.userDetail(userId), "用户服务调用失败");
    }

    public void banUser(Long adminId, Long userId, BanUserRequest request) {
        data(userAdminClient.banUser(adminId, userId, request), "用户服务调用失败");
        logOperation(adminId, "BAN_USER", "USER", userId, request.getReason());
    }

    public void unbanUser(Long adminId, Long userId) {
        data(userAdminClient.unbanUser(userId), "用户服务调用失败");
        logOperation(adminId, "UNBAN_USER", "USER", userId, null);
    }

    public Object merchantApplies(Integer page, Integer size, Integer status) {
        return data(userAdminClient.merchantApplies(page, size, status), "用户服务调用失败");
    }

    public Object merchantApplyDetail(Long applyId) {
        return data(userAdminClient.merchantApplyDetail(applyId), "用户服务调用失败");
    }

    public void reviewMerchantApply(Long adminId, Long applyId, ReviewRequest request) {
        data(userAdminClient.reviewMerchantApply(adminId, applyId, request), "用户服务调用失败");
        logOperation(adminId, "REVIEW_MERCHANT_APPLY", "MERCHANT_APPLY", applyId, request.getComment());
    }

    public Object pendingActivities(Integer page, Integer size) {
        return data(activityAdminClient.pendingActivities(page, size), "活动服务调用失败");
    }

    public Object activities(Integer page, Integer size, Integer status, String keyword) {
        return data(activityAdminClient.activities(page, size, status, keyword), "活动服务调用失败");
    }

    public Object activityDetail(Long activityId) {
        return data(activityAdminClient.activityDetail(activityId), "活动服务调用失败");
    }

    public void reviewActivity(Long adminId, Long activityId, ReviewRequest request) {
        data(activityAdminClient.review(adminId, activityId, request), "活动服务调用失败");
        logOperation(adminId, "REVIEW_ACTIVITY", "ACTIVITY", activityId, request.getComment());
    }

    public void offlineActivity(Long adminId, Long activityId, OfflineActivityRequest request) {
        data(activityAdminClient.offline(adminId, activityId, request), "活动服务调用失败");
        logOperation(adminId, "OFFLINE_ACTIVITY", "ACTIVITY", activityId, request.getReason());
    }

    public void restoreActivity(Long adminId, Long activityId) {
        data(activityAdminClient.restore(activityId), "活动服务调用失败");
        logOperation(adminId, "RESTORE_ACTIVITY", "ACTIVITY", activityId, null);
    }

    public Object teams(Integer page, Integer size, Integer status, String keyword) {
        return data(socialAdminClient.teams(page, size, status, keyword), "社群服务调用失败");
    }

    public Object teamDetail(Long teamId) {
        return data(socialAdminClient.teamDetail(teamId), "社群服务调用失败");
    }

    public Object teamMembers(Long teamId) {
        return data(socialAdminClient.teamMembers(teamId), "社群服务调用失败");
    }

    public void disableTeam(Long adminId, Long teamId, DisableTeamRequest request) {
        data(socialAdminClient.disableTeam(adminId, teamId, request), "社群服务调用失败");
        logOperation(adminId, "DISABLE_TEAM", "TEAM", teamId, request.getReason());
    }

    public void restoreTeam(Long adminId, Long teamId) {
        data(socialAdminClient.restoreTeam(teamId, new AdminRestoreTeamRequest(adminId)), "社群服务调用失败");
        logOperation(adminId, "RESTORE_TEAM", "TEAM", teamId, null);
    }

    private Object data(com.onlyfriends.common.response.Result<?> result, String fallbackMessage) {
        if (result == null || !Integer.valueOf(200).equals(result.getCode())) {
            String message = result == null ? fallbackMessage : result.getMessage();
            throw new BizException(ResultCode.INTERNAL_ERROR.getCode(), message);
        }
        return result.getData();
    }

    private void logOperation(Long adminId, String operationType, String targetType, Long targetId, String detail) {
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(adminId);
        log.setOperationType(operationType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        operationLogMapper.insert(log);
    }
}
