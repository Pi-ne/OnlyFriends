package com.ququ.im.controller;

import com.ququ.common.response.Result;
import com.ququ.im.dto.response.OnlineStatusResponse;
import com.ququ.im.security.CurrentUser;
import com.ququ.im.service.ImStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/im")
public class ImPresenceController {
    private final ImStateService stateService;

    @GetMapping("/users/{userId}/online")
    public Result<OnlineStatusResponse> online(@PathVariable Long userId) {
        return Result.success(new OnlineStatusResponse(userId, stateService.isOnline(userId)));
    }

    @PostMapping("/online/heartbeat")
    public Result<Void> heartbeat(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser != null) {
            stateService.refreshOnline(currentUser.getUserId(), "rest-heartbeat");
        }
        return Result.success();
    }
}
