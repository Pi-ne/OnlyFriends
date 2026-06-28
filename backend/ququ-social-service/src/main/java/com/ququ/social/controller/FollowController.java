package com.ququ.social.controller;

import com.ququ.common.exception.BizException;
import com.ququ.common.response.Result;
import com.ququ.common.response.ResultCode;
import com.ququ.social.dto.response.UserRelationResponse;
import com.ququ.social.security.CurrentUser;
import com.ququ.social.service.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/follows")
public class FollowController {
    private final SocialService socialService;

    @PostMapping("/{userId}")
    public Result<Void> follow(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long userId) {
        socialService.follow(requireUser(currentUser).getUserId(), userId);
        return Result.success();
    }

    @DeleteMapping("/{userId}")
    public Result<Void> unfollow(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long userId) {
        socialService.unfollow(requireUser(currentUser).getUserId(), userId);
        return Result.success();
    }

    @GetMapping("/following")
    public Result<List<UserRelationResponse>> following(@AuthenticationPrincipal CurrentUser currentUser) {
        return Result.success(socialService.following(requireUser(currentUser).getUserId()));
    }

    @GetMapping("/followers")
    public Result<List<UserRelationResponse>> followers(@AuthenticationPrincipal CurrentUser currentUser) {
        return Result.success(socialService.followers(requireUser(currentUser).getUserId()));
    }

    private CurrentUser requireUser(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return currentUser;
    }
}
