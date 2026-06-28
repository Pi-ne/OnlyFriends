package com.ququ.user.service;

import com.ququ.common.dto.UserBasicDTO;
import com.ququ.user.dto.request.UpdateProfileRequest;
import com.ququ.user.dto.response.PublicUserProfileResponse;
import com.ququ.user.dto.response.UserProfileResponse;
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
