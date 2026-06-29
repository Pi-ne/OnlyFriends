package com.onlyfriends.activity.controller;

import com.onlyfriends.activity.dto.response.NotificationResponse;
import com.onlyfriends.activity.security.CurrentUser;
import com.onlyfriends.activity.service.ActivityService;
import com.onlyfriends.common.dto.PageResult;
import com.onlyfriends.common.exception.BizException;
import com.onlyfriends.common.response.Result;
import com.onlyfriends.common.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final ActivityService activityService;

    @GetMapping
    public Result<PageResult<NotificationResponse>> notifications(@AuthenticationPrincipal CurrentUser currentUser,
                                                                  @RequestParam(defaultValue = "1") Integer page,
                                                                  @RequestParam(defaultValue = "20") Integer size,
                                                                  @RequestParam(required = false) Boolean unreadOnly) {
        return Result.success(activityService.notifications(requireUser(currentUser).getUserId(), page, size, unreadOnly));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long id) {
        activityService.markNotificationRead(requireUser(currentUser).getUserId(), id);
        return Result.success();
    }

    private CurrentUser requireUser(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return currentUser;
    }
}
