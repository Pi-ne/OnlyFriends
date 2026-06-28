package com.ququ.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ququ.common.dto.UserBasicDTO;
import com.ququ.common.exception.BizException;
import com.ququ.common.response.ResultCode;
import com.ququ.common.storage.FileStorageService;
import com.ququ.user.dto.request.UpdateProfileRequest;
import com.ququ.user.dto.response.PublicUserProfileResponse;
import com.ququ.user.dto.response.UserProfileResponse;
import com.ququ.user.entity.User;
import com.ququ.user.mapper.UserMapper;
import com.ququ.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final int STATUS_NORMAL = 1;
    private static final int STATUS_BANNED = 2;

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

    @Override
    public UserProfileResponse getUserProfile(Long userId) {
        return toProfile(getUserOrThrow(userId));
    }

    @Override
    public PublicUserProfileResponse getPublicUserProfile(Long userId) {
        User user = getUserOrThrow(userId);
        PublicUserProfileResponse response = new PublicUserProfileResponse();
        response.setUserId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setGender(user.getGender());
        response.setBio(user.getBio());
        response.setInterestTags(fromJson(user.getInterestTags()));
        response.setUserType(user.getUserType());
        return response;
    }

    @Override
    @Transactional
    public void updateProfile(Long currentUserId, UpdateProfileRequest request) {
        User user = getUserOrThrow(currentUserId);
        if (StringUtils.hasText(request.getNickname())) {
            String nickname = request.getNickname().trim();
            Long sameNicknameCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getNickname, nickname)
                    .ne(User::getId, currentUserId));
            if (sameNicknameCount > 0) {
                throw new BizException(ResultCode.NICKNAME_ALREADY_EXISTS);
            }
            user.setNickname(nickname);
        }
        if (request.getGender() != null) {
            if (!Set.of(0, 1, 2).contains(request.getGender())) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(), "性别只能为0、1或2");
            }
            user.setGender(request.getGender());
        }
        if (request.getBirthday() != null) {
            user.setBirthday(request.getBirthday());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getInterestTags() != null) {
            user.setInterestTags(toJson(request.getInterestTags()));
        }
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        User user = getUserOrThrow(userId);
        String avatarUrl = fileStorageService.upload("avatar", file);
        user.setAvatarUrl(avatarUrl);
        userMapper.updateById(user);
        return avatarUrl;
    }

    @Override
    public List<UserBasicDTO> getUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, ids)).stream()
                .map(user -> new UserBasicDTO(user.getId(), user.getNickname(), user.getAvatarUrl(), user.getUserType()))
                .toList();
    }

    @Override
    public Boolean isUserValid(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && Integer.valueOf(STATUS_NORMAL).equals(user.getStatus());
    }

    @Override
    public Integer getUserCredit(Long userId) {
        return getUserOrThrow(userId).getCreditScore();
    }

    @Override
    @Transactional
    public void deductCredit(Long userId, Integer amount) {
        if (amount == null || amount <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "扣减分值必须大于0");
        }
        User user = getUserOrThrow(userId);
        user.setCreditScore(Math.max(0, user.getCreditScore() - amount));
        if (user.getCreditScore() <= 0) {
            user.setStatus(STATUS_BANNED);
        }
        userMapper.updateById(user);
    }

    private User getUserOrThrow(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    private UserProfileResponse toProfile(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setGender(user.getGender());
        response.setBirthday(user.getBirthday());
        response.setBio(user.getBio());
        response.setInterestTags(fromJson(user.getInterestTags()));
        response.setUserType(user.getUserType());
        response.setStatus(user.getStatus());
        response.setCreditScore(user.getCreditScore());
        return response;
    }

    private String toJson(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException ex) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "兴趣标签格式错误");
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
