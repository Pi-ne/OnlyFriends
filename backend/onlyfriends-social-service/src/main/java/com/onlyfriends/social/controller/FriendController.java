package com.onlyfriends.social.controller;

import com.onlyfriends.common.exception.BizException;
import com.onlyfriends.common.response.Result;
import com.onlyfriends.common.response.ResultCode;
import com.onlyfriends.social.dto.request.FriendApplyRequest;
import com.onlyfriends.social.dto.request.FriendSettingRequest;
import com.onlyfriends.social.dto.request.ReviewRequest;
import com.onlyfriends.social.dto.response.FriendApplyResponse;
import com.onlyfriends.social.dto.response.UserRelationResponse;
import com.onlyfriends.social.security.CurrentUser;
import com.onlyfriends.social.service.SocialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/friends")
public class FriendController {
    private final SocialService socialService;

    @PostMapping("/{userId}/applies")
    public Result<Map<String, Long>> apply(@AuthenticationPrincipal CurrentUser currentUser,
                                           @PathVariable Long userId,
                                           @Valid @RequestBody(required = false) FriendApplyRequest request) {
        Long applyId = socialService.applyFriend(requireUser(currentUser).getUserId(), userId, request);
        return Result.success(Map.of("applyId", applyId));
    }

    @GetMapping("/applies")
    public Result<List<FriendApplyResponse>> applies(@AuthenticationPrincipal CurrentUser currentUser,
                                                     @RequestParam(defaultValue = "received") String type) {
        return Result.success(socialService.friendApplies(requireUser(currentUser).getUserId(), type));
    }

    @PutMapping("/applies/{id}")
    public Result<Void> reviewApply(@AuthenticationPrincipal CurrentUser currentUser,
                                    @PathVariable Long id,
                                    @Valid @RequestBody ReviewRequest request) {
        socialService.reviewFriendApply(requireUser(currentUser).getUserId(), id, request);
        return Result.success();
    }

    @GetMapping
    public Result<List<UserRelationResponse>> friends(@AuthenticationPrincipal CurrentUser currentUser) {
        return Result.success(socialService.friends(requireUser(currentUser).getUserId()));
    }

    @PutMapping("/{userId}/setting")
    public Result<Void> updateSetting(@AuthenticationPrincipal CurrentUser currentUser,
                                      @PathVariable Long userId,
                                      @Valid @RequestBody FriendSettingRequest request) {
        socialService.updateFriendSetting(requireUser(currentUser).getUserId(), userId, request);
        return Result.success();
    }

    @DeleteMapping("/{userId}")
    public Result<Void> deleteFriend(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long userId) {
        socialService.deleteFriend(requireUser(currentUser).getUserId(), userId);
        return Result.success();
    }

    private CurrentUser requireUser(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return currentUser;
    }
}
