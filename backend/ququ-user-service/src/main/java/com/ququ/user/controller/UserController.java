package com.ququ.user.controller;

import com.ququ.common.response.Result;
import com.ququ.user.dto.request.UpdateProfileRequest;
import com.ququ.user.dto.response.PublicUserProfileResponse;
import com.ququ.user.dto.response.UserProfileResponse;
import com.ququ.user.security.CurrentUser;
import com.ququ.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/me/profile")
    public Result<UserProfileResponse> getMyProfile(@AuthenticationPrincipal CurrentUser currentUser) {
        return Result.success(userService.getUserProfile(currentUser.getUserId()));
    }

    @PutMapping("/me/profile")
    public Result<Void> updateMyProfile(@AuthenticationPrincipal CurrentUser currentUser,
                                        @Valid @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(currentUser.getUserId(), request);
        return Result.success("更新成功", null);
    }

    @PostMapping("/me/avatar")
    public Result<Map<String, String>> uploadAvatar(@AuthenticationPrincipal CurrentUser currentUser,
                                                    @RequestParam("file") MultipartFile file) {
        String avatarUrl = userService.uploadAvatar(currentUser.getUserId(), file);
        return Result.success(Map.of("avatarUrl", avatarUrl));
    }

    @GetMapping("/{id}")
    public Result<PublicUserProfileResponse> getUserProfile(@PathVariable Long id) {
        return Result.success(userService.getPublicUserProfile(id));
    }
}
