package com.ququ.user.controller.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ququ.common.dto.PageResult;
import com.ququ.common.exception.BizException;
import com.ququ.common.response.Result;
import com.ququ.common.response.ResultCode;
import com.ququ.user.dto.request.AdminBanUserRequest;
import com.ququ.user.dto.request.AdminReviewMerchantApplyRequest;
import com.ququ.user.dto.response.AdminMerchantApplyResponse;
import com.ququ.user.dto.response.AdminUserResponse;
import com.ququ.user.entity.MerchantApply;
import com.ququ.user.entity.MerchantInfo;
import com.ququ.user.entity.User;
import com.ququ.user.entity.UserBanRecord;
import com.ququ.user.mapper.MerchantApplyMapper;
import com.ququ.user.mapper.MerchantInfoMapper;
import com.ququ.user.mapper.UserBanRecordMapper;
import com.ququ.user.mapper.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/admin")
public class AdminUserInternalController {
    private static final int USER_STATUS_NORMAL = 1;
    private static final int USER_STATUS_BANNED = 2;
    private static final int MERCHANT_PENDING = 0;
    private static final int MERCHANT_APPROVED = 1;
    private static final int MERCHANT_REJECTED = 2;

    private final UserMapper userMapper;
    private final UserBanRecordMapper userBanRecordMapper;
    private final MerchantApplyMapper merchantApplyMapper;
    private final MerchantInfoMapper merchantInfoMapper;
    private final ObjectMapper objectMapper;

    @GetMapping("/users")
    public Result<PageResult<AdminUserResponse>> users(@RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "20") Integer size,
                                                       @RequestParam(required = false) Integer userType,
                                                       @RequestParam(required = false) Integer status,
                                                       @RequestParam(required = false) String email,
                                                       @RequestParam(required = false) String nickname,
                                                       @RequestParam(required = false) String keyword) {
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null ? 20 : Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userType != null, User::getUserType, userType);
        wrapper.eq(status != null, User::getStatus, status);
        wrapper.like(StringUtils.hasText(email), User::getEmail, email);
        wrapper.like(StringUtils.hasText(nickname), User::getNickname, nickname);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getEmail, keyword).or().like(User::getNickname, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);
        List<User> all = userMapper.selectList(wrapper);
        List<AdminUserResponse> rows = all.stream()
                .skip((long) (current - 1) * pageSize)
                .limit(pageSize)
                .map(this::toUserResponse)
                .toList();
        return Result.success(new PageResult<>(rows, (long) all.size(), (long) current, (long) pageSize));
    }

    @GetMapping("/users/{id}")
    public Result<AdminUserResponse> userDetail(@PathVariable Long id) {
        return Result.success(toUserResponse(getUserOrThrow(id)));
    }

    @PostMapping("/users/{id}/ban")
    @Transactional
    public Result<Void> banUser(@PathVariable Long id, @Valid @RequestBody AdminBanUserRequest request) {
        User user = getUserOrThrow(id);
        user.setStatus(USER_STATUS_BANNED);
        user.setBanExpireAt(request.getBanExpireAt());
        userMapper.updateById(user);

        UserBanRecord record = new UserBanRecord();
        record.setUserId(id);
        record.setAdminId(request.getAdminId());
        record.setReason(request.getReason().trim());
        record.setBanExpireAt(request.getBanExpireAt());
        userBanRecordMapper.insert(record);
        return Result.success();
    }

    @PostMapping("/users/{id}/unban")
    @Transactional
    public Result<Void> unbanUser(@PathVariable Long id) {
        User user = getUserOrThrow(id);
        user.setStatus(USER_STATUS_NORMAL);
        user.setBanExpireAt(null);
        userMapper.updateById(user);
        return Result.success();
    }

    @GetMapping("/merchant-applies")
    public Result<PageResult<AdminMerchantApplyResponse>> merchantApplies(@RequestParam(defaultValue = "1") Integer page,
                                                                          @RequestParam(defaultValue = "20") Integer size,
                                                                          @RequestParam(required = false) Integer status) {
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null ? 20 : Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<MerchantApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, MerchantApply::getStatus, status);
        wrapper.orderByDesc(MerchantApply::getCreatedAt);
        List<MerchantApply> all = merchantApplyMapper.selectList(wrapper);
        List<AdminMerchantApplyResponse> rows = all.stream()
                .skip((long) (current - 1) * pageSize)
                .limit(pageSize)
                .map(this::toMerchantApplyResponse)
                .toList();
        return Result.success(new PageResult<>(rows, (long) all.size(), (long) current, (long) pageSize));
    }

    @GetMapping("/merchant-applies/{id}")
    public Result<AdminMerchantApplyResponse> merchantApplyDetail(@PathVariable Long id) {
        MerchantApply apply = merchantApplyMapper.selectById(id);
        if (apply == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "商家申请不存在");
        }
        return Result.success(toMerchantApplyResponse(apply));
    }

    @PutMapping("/merchant-applies/{id}/review")
    @Transactional
    public Result<Void> reviewMerchantApply(@PathVariable Long id,
                                            @Valid @RequestBody AdminReviewMerchantApplyRequest request) {
        MerchantApply apply = merchantApplyMapper.selectById(id);
        if (apply == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "商家申请不存在");
        }
        if (!Integer.valueOf(MERCHANT_PENDING).equals(apply.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "该申请已审核");
        }
        if (Integer.valueOf(MERCHANT_REJECTED).equals(request.getAction()) && !StringUtils.hasText(request.getReason())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "驳回必须填写原因");
        }
        if (!Integer.valueOf(MERCHANT_APPROVED).equals(request.getAction())
                && !Integer.valueOf(MERCHANT_REJECTED).equals(request.getAction())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "审核结果只能为通过或驳回");
        }

        apply.setStatus(request.getAction());
        apply.setReviewerId(request.getAdminId());
        apply.setReviewedAt(LocalDateTime.now());
        apply.setRejectReason(Integer.valueOf(MERCHANT_REJECTED).equals(request.getAction()) ? request.getReason().trim() : null);
        merchantApplyMapper.updateById(apply);

        if (Integer.valueOf(MERCHANT_APPROVED).equals(request.getAction())) {
            approveMerchant(apply);
        }
        return Result.success();
    }

    private void approveMerchant(MerchantApply apply) {
        User user = getUserOrThrow(apply.getUserId());
        user.setUserType(1);
        userMapper.updateById(user);

        Long count = merchantInfoMapper.selectCount(new LambdaQueryWrapper<MerchantInfo>().eq(MerchantInfo::getUserId, apply.getUserId()));
        if (count > 0) {
            return;
        }
        MerchantInfo info = new MerchantInfo();
        info.setUserId(apply.getUserId());
        info.setMerchantName(apply.getMerchantName());
        info.setFocusTags(apply.getFocusTags());
        info.setLicenseUrl(apply.getLicenseUrl());
        merchantInfoMapper.insert(info);
    }

    private User getUserOrThrow(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    private AdminUserResponse toUserResponse(User user) {
        AdminUserResponse response = new AdminUserResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setUserType(user.getUserType());
        response.setStatus(user.getStatus());
        response.setCreditScore(user.getCreditScore());
        response.setBanExpireAt(user.getBanExpireAt());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    private AdminMerchantApplyResponse toMerchantApplyResponse(MerchantApply apply) {
        AdminMerchantApplyResponse response = new AdminMerchantApplyResponse();
        response.setApplyId(apply.getId());
        response.setUserId(apply.getUserId());
        response.setMerchantName(apply.getMerchantName());
        response.setLicenseUrl(apply.getLicenseUrl());
        response.setFocusTags(fromJson(apply.getFocusTags()));
        response.setStatus(apply.getStatus());
        response.setRejectReason(apply.getRejectReason());
        response.setReviewerId(apply.getReviewerId());
        response.setReviewedAt(apply.getReviewedAt());
        response.setCreatedAt(apply.getCreatedAt());
        return response;
    }

    private List<String> fromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return Collections.emptyList();
        }
    }
}
