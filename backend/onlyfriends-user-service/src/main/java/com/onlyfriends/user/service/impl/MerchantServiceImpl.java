package com.onlyfriends.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyfriends.common.exception.BizException;
import com.onlyfriends.common.response.ResultCode;
import com.onlyfriends.user.dto.request.MerchantApplyRequest;
import com.onlyfriends.user.dto.response.MerchantInfoResponse;
import com.onlyfriends.user.dto.response.MerchantApplyStatusResponse;
import com.onlyfriends.user.entity.MerchantApply;
import com.onlyfriends.user.entity.MerchantInfo;
import com.onlyfriends.user.entity.User;
import com.onlyfriends.user.mapper.MerchantApplyMapper;
import com.onlyfriends.user.mapper.MerchantInfoMapper;
import com.onlyfriends.user.mapper.UserMapper;
import com.onlyfriends.user.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {
    private static final int APPLY_PENDING = 0;

    private final MerchantApplyMapper merchantApplyMapper;
    private final MerchantInfoMapper merchantInfoMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Long apply(Long userId, MerchantApplyRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        MerchantInfo info = merchantInfoMapper.selectOne(new LambdaQueryWrapper<MerchantInfo>().eq(MerchantInfo::getUserId, userId));
        if (info != null || Integer.valueOf(1).equals(user.getUserType())) {
            throw new BizException(ResultCode.MERCHANT_ALREADY_EXISTS);
        }
        Long pendingCount = merchantApplyMapper.selectCount(new LambdaQueryWrapper<MerchantApply>()
                .eq(MerchantApply::getUserId, userId)
                .eq(MerchantApply::getStatus, APPLY_PENDING));
        if (pendingCount > 0) {
            throw new BizException(ResultCode.MERCHANT_APPLY_PENDING);
        }
        MerchantApply apply = new MerchantApply();
        apply.setUserId(userId);
        apply.setMerchantName(request.getMerchantName().trim());
        apply.setLicenseUrl(request.getLicenseUrl().trim());
        if (request.getFocusTags() != null) {
            apply.setFocusTags(toJson(request.getFocusTags()));
        }
        apply.setStatus(APPLY_PENDING);
        merchantApplyMapper.insert(apply);
        return apply.getId();
    }

    @Override
    public MerchantApplyStatusResponse getApplyStatus(Long userId) {
        MerchantApply apply = merchantApplyMapper.selectOne(new LambdaQueryWrapper<MerchantApply>()
                .eq(MerchantApply::getUserId, userId)
                .orderByDesc(MerchantApply::getCreatedAt)
                .last("LIMIT 1"));
        if (apply == null) {
            return null;
        }
        MerchantApplyStatusResponse response = new MerchantApplyStatusResponse();
        response.setApplyId(apply.getId());
        response.setMerchantName(apply.getMerchantName());
        response.setLicenseUrl(apply.getLicenseUrl());
        response.setFocusTags(fromJson(apply.getFocusTags()));
        response.setStatus(apply.getStatus());
        response.setRejectReason(apply.getRejectReason());
        response.setReviewedAt(apply.getReviewedAt());
        response.setCreatedAt(apply.getCreatedAt());
        return response;
    }

    @Override
    public MerchantInfoResponse getMerchantInfo(Long userId) {
        MerchantInfo info = merchantInfoMapper.selectOne(new LambdaQueryWrapper<MerchantInfo>().eq(MerchantInfo::getUserId, userId));
        if (info == null) {
            return null;
        }
        MerchantInfoResponse response = new MerchantInfoResponse();
        response.setUserId(info.getUserId());
        response.setMerchantName(info.getMerchantName());
        response.setMerchantNick(info.getMerchantNick());
        response.setFocusTags(fromJson(info.getFocusTags()));
        response.setLicenseUrl(info.getLicenseUrl());
        return response;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "商家标签格式错误");
        }
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
