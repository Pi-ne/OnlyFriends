package com.ququ.activity.controller;

import com.ququ.activity.dto.response.NotificationResponse;
import com.ququ.activity.security.CurrentUser;
import com.ququ.activity.service.ActivityService;
import com.ququ.common.dto.PageResult;
import com.ququ.common.exception.BizException;
import com.ququ.common.response.Result;
import com.ququ.common.response.ResultCode;
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
