package com.onlyfriends.activity.controller;

import com.onlyfriends.activity.dto.request.ActivityCreateRequest;
import com.onlyfriends.activity.dto.request.ActivityCheckinRequest;
import com.onlyfriends.activity.dto.request.ActivityCommentRequest;
import com.onlyfriends.activity.dto.request.ActivityQueryRequest;
import com.onlyfriends.activity.dto.request.ActivitySummaryRequest;
import com.onlyfriends.activity.dto.request.ActivityUpdateRequest;
import com.onlyfriends.activity.dto.response.ActivityCheckinQrcodeResponse;
import com.onlyfriends.activity.dto.response.ActivityCheckinResponse;
import com.onlyfriends.activity.dto.response.ActivityCommentResponse;
import com.onlyfriends.activity.dto.response.ActivityCreateResponse;
import com.onlyfriends.activity.dto.response.ActivityDetailResponse;
import com.onlyfriends.activity.dto.response.ActivityListItemResponse;
import com.onlyfriends.activity.dto.response.ActivityRegistrationItemResponse;
import com.onlyfriends.activity.dto.response.ActivityRegistrationStatusResponse;
import com.onlyfriends.activity.dto.response.ActivitySummaryResponse;
import com.onlyfriends.activity.dto.response.ActivityTagResponse;
import com.onlyfriends.activity.dto.response.ActivityTemplateResponse;
import com.onlyfriends.activity.dto.response.NotificationResponse;
import com.onlyfriends.activity.security.CurrentUser;
import com.onlyfriends.activity.service.ActivityService;
import com.onlyfriends.common.dto.PageResult;
import com.onlyfriends.common.exception.BizException;
import com.onlyfriends.common.response.Result;
import com.onlyfriends.common.response.ResultCode;
import com.onlyfriends.common.storage.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/activities")
public class ActivityController {
    private final ActivityService activityService;
    private final FileStorageService fileStorageService;

    @PostMapping
    public Result<ActivityCreateResponse> create(@AuthenticationPrincipal CurrentUser currentUser,
                                                 @Valid @RequestBody ActivityCreateRequest request) {
        return Result.success(activityService.create(requireUser(currentUser).getUserId(), request));
    }

    @PutMapping("/{id:\\d+}")
    public Result<ActivityCreateResponse> updateDraft(@AuthenticationPrincipal CurrentUser currentUser,
                                                      @PathVariable Long id,
                                                      @Valid @RequestBody ActivityUpdateRequest request) {
        return Result.success(activityService.updateDraft(id, requireUser(currentUser).getUserId(), request));
    }

    @PostMapping("/{id:\\d+}/submit")
    public Result<ActivityCreateResponse> submit(@AuthenticationPrincipal CurrentUser currentUser,
                                                 @PathVariable Long id) {
        return Result.success(activityService.submit(id, requireUser(currentUser).getUserId()));
    }

    @PostMapping("/{id:\\d+}/clone")
    public Result<ActivityCreateResponse> cloneActivity(@AuthenticationPrincipal CurrentUser currentUser,
                                                        @PathVariable Long id) {
        return Result.success(activityService.cloneActivity(id, requireUser(currentUser).getUserId()));
    }

    @GetMapping
    public Result<PageResult<ActivityListItemResponse>> list(@ModelAttribute ActivityQueryRequest request) {
        return Result.success(activityService.list(request));
    }

    @GetMapping("/nearby")
    public Result<PageResult<ActivityListItemResponse>> nearby(@AuthenticationPrincipal CurrentUser currentUser,
                                                               @ModelAttribute ActivityQueryRequest request) {
        requireUser(currentUser);
        request.setTab("nearby");
        return Result.success(activityService.list(request));
    }

    @GetMapping("/registered")
    public Result<PageResult<ActivityListItemResponse>> registered(@AuthenticationPrincipal CurrentUser currentUser,
                                                                   @RequestParam(defaultValue = "1") Integer page,
                                                                   @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(activityService.registeredActivities(requireUser(currentUser).getUserId(), page, size));
    }

    @GetMapping("/{id:\\d+}")
    public Result<ActivityDetailResponse> detail(@PathVariable Long id) {
        return Result.success(activityService.detail(id));
    }

    @GetMapping("/templates")
    public Result<List<ActivityTemplateResponse>> templates(@AuthenticationPrincipal CurrentUser currentUser) {
        requireUser(currentUser);
        return Result.success(activityService.templates());
    }

    @GetMapping("/tags")
    public Result<List<ActivityTagResponse>> tags(@RequestParam(required = false) String category,
                                                  @RequestParam(required = false) Integer limit) {
        return Result.success(activityService.tags(category, limit));
    }

    @PostMapping("/{id:\\d+}/register")
    public Result<ActivityRegistrationStatusResponse> register(@AuthenticationPrincipal CurrentUser currentUser,
                                                               @PathVariable Long id) {
        return Result.success(activityService.register(id, requireUser(currentUser).getUserId()));
    }

    @DeleteMapping("/{id:\\d+}/register")
    public Result<ActivityRegistrationStatusResponse> cancelRegistration(@AuthenticationPrincipal CurrentUser currentUser,
                                                                         @PathVariable Long id) {
        return Result.success(activityService.cancelRegistration(id, requireUser(currentUser).getUserId()));
    }

    @GetMapping("/{id:\\d+}/registration/me")
    public Result<ActivityRegistrationStatusResponse> myRegistrationStatus(@AuthenticationPrincipal CurrentUser currentUser,
                                                                           @PathVariable Long id) {
        return Result.success(activityService.myRegistrationStatus(id, requireUser(currentUser).getUserId()));
    }

    @GetMapping("/{id:\\d+}/registrations")
    public Result<List<ActivityRegistrationItemResponse>> registrations(@AuthenticationPrincipal CurrentUser currentUser,
                                                                        @PathVariable Long id) {
        return Result.success(activityService.registrations(id, requireUser(currentUser).getUserId()));
    }

    @PostMapping("/images")
    public Result<Map<String, String>> uploadImage(@AuthenticationPrincipal CurrentUser currentUser,
                                                   @RequestParam("file") MultipartFile file) {
        requireUser(currentUser);
        String imageUrl = fileStorageService.upload("activity-image", file);
        return Result.success(Map.of("imageUrl", imageUrl));
    }

    @GetMapping("/{id:\\d+}/checkin/qrcode")
    public Result<ActivityCheckinQrcodeResponse> checkinQrcode(@AuthenticationPrincipal CurrentUser currentUser,
                                                               @PathVariable Long id) {
        return Result.success(activityService.generateCheckinQrcode(id, requireUser(currentUser).getUserId()));
    }

    @PostMapping("/{id:\\d+}/checkin")
    public Result<ActivityCheckinResponse> checkin(@AuthenticationPrincipal CurrentUser currentUser,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody ActivityCheckinRequest request) {
        return Result.success(activityService.checkin(id, requireUser(currentUser).getUserId(), request));
    }

    @PostMapping("/{id:\\d+}/summary")
    public Result<ActivitySummaryResponse> publishSummary(@AuthenticationPrincipal CurrentUser currentUser,
                                                          @PathVariable Long id,
                                                          @Valid @RequestBody ActivitySummaryRequest request) {
        return Result.success(activityService.publishSummary(id, requireUser(currentUser).getUserId(), request));
    }

    @GetMapping("/{id:\\d+}/summary")
    public Result<ActivitySummaryResponse> summary(@PathVariable Long id) {
        return Result.success(activityService.summary(id));
    }

    @PostMapping("/{id:\\d+}/comments")
    public Result<ActivityCommentResponse> publishComment(@AuthenticationPrincipal CurrentUser currentUser,
                                                          @PathVariable Long id,
                                                          @Valid @RequestBody ActivityCommentRequest request) {
        return Result.success(activityService.publishComment(id, requireUser(currentUser).getUserId(), request));
    }

    @GetMapping("/{id:\\d+}/comments")
    public Result<PageResult<ActivityCommentResponse>> comments(@PathVariable Long id,
                                                                @RequestParam(defaultValue = "1") Integer page,
                                                                @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(activityService.comments(id, page, size));
    }

    @GetMapping("/notifications")
    public Result<PageResult<NotificationResponse>> notifications(@AuthenticationPrincipal CurrentUser currentUser,
                                                                  @RequestParam(defaultValue = "1") Integer page,
                                                                  @RequestParam(defaultValue = "20") Integer size,
                                                                  @RequestParam(required = false) Boolean unreadOnly) {
        return Result.success(activityService.notifications(requireUser(currentUser).getUserId(), page, size, unreadOnly));
    }

    @PutMapping("/notifications/{id}/read")
    public Result<Void> markNotificationRead(@AuthenticationPrincipal CurrentUser currentUser,
                                             @PathVariable Long id) {
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
