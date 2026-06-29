package com.onlyfriends.activity.service;

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
import com.onlyfriends.common.dto.PageResult;

import java.util.List;

public interface ActivityService {
    ActivityCreateResponse create(Long creatorId, ActivityCreateRequest request);

    ActivityCreateResponse updateDraft(Long activityId, Long creatorId, ActivityUpdateRequest request);

    ActivityCreateResponse submit(Long activityId, Long creatorId);

    ActivityCreateResponse cloneActivity(Long activityId, Long creatorId);

    PageResult<ActivityListItemResponse> list(ActivityQueryRequest request);

    PageResult<ActivityListItemResponse> registeredActivities(Long userId, Integer page, Integer size);

    ActivityDetailResponse detail(Long activityId);

    List<ActivityTemplateResponse> templates();

    List<ActivityTagResponse> tags(String category, Integer limit);

    ActivityRegistrationStatusResponse register(Long activityId, Long userId);

    ActivityRegistrationStatusResponse cancelRegistration(Long activityId, Long userId);

    ActivityRegistrationStatusResponse myRegistrationStatus(Long activityId, Long userId);

    List<ActivityRegistrationItemResponse> registrations(Long activityId, Long creatorId);

    ActivityCheckinQrcodeResponse generateCheckinQrcode(Long activityId, Long creatorId);

    ActivityCheckinResponse checkin(Long activityId, Long userId, ActivityCheckinRequest request);

    ActivitySummaryResponse publishSummary(Long activityId, Long creatorId, ActivitySummaryRequest request);

    ActivitySummaryResponse summary(Long activityId);

    ActivityCommentResponse publishComment(Long activityId, Long userId, ActivityCommentRequest request);

    PageResult<ActivityCommentResponse> comments(Long activityId, Integer page, Integer size);

    PageResult<NotificationResponse> notifications(Long userId, Integer page, Integer size, Boolean unreadOnly);

    void markNotificationRead(Long userId, Long notificationId);

    int transitionActivityStatuses();

    int expirePendingWaitlists();
}
