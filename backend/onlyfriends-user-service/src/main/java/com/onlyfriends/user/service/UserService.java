package com.onlyfriends.user.service;

import com.onlyfriends.common.dto.UserBasicDTO;
import com.onlyfriends.user.dto.request.UpdateProfileRequest;
import com.onlyfriends.user.dto.response.PublicUserProfileResponse;
import com.onlyfriends.user.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    UserProfileResponse getUserProfile(Long userId);

    PublicUserProfileResponse getPublicUserProfile(Long userId);

    void updateProfile(Long currentUserId, UpdateProfileRequest request);

    String uploadAvatar(Long userId, MultipartFile file);

    List<UserBasicDTO> getUsersByIds(List<Long> ids);

    Boolean isUserValid(Long userId);

    Integer getUserCredit(Long userId);

    void deductCredit(Long userId, Integer amount);
}
