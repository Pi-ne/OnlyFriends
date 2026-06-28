package com.ququ.activity.controller;

import com.ququ.activity.dto.request.ActivityCreateRequest;
import com.ququ.activity.dto.request.ActivityCheckinRequest;
import com.ququ.activity.dto.request.ActivityCommentRequest;
import com.ququ.activity.dto.request.ActivityQueryRequest;
import com.ququ.activity.dto.request.ActivitySummaryRequest;
import com.ququ.activity.dto.request.ActivityUpdateRequest;
import com.ququ.activity.dto.response.ActivityCheckinQrcodeResponse;
import com.ququ.activity.dto.response.ActivityCheckinResponse;
import com.ququ.activity.dto.response.ActivityCommentResponse;
import com.ququ.activity.dto.response.ActivityCreateResponse;
import com.ququ.activity.dto.response.ActivityDetailResponse;
import com.ququ.activity.dto.response.ActivityListItemResponse;
import com.ququ.activity.dto.response.ActivityRegistrationItemResponse;
import com.ququ.activity.dto.response.ActivityRegistrationStatusResponse;
import com.ququ.activity.dto.response.ActivitySummaryResponse;
import com.ququ.activity.dto.response.ActivityTagResponse;
import com.ququ.activity.dto.response.ActivityTemplateResponse;
import com.ququ.activity.dto.response.NotificationResponse;
import com.ququ.activity.security.CurrentUser;
import com.ququ.activity.service.ActivityService;
import com.ququ.common.dto.PageResult;
import com.ququ.common.exception.BizException;
import com.ququ.common.response.Result;
import com.ququ.common.response.ResultCode;
import com.ququ.common.storage.FileStorageService;
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
