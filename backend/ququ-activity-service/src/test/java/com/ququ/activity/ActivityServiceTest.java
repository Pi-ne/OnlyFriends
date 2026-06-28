package com.ququ.activity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ququ.activity.dto.request.ActivityCheckinRequest;
import com.ququ.activity.dto.request.ActivityCommentRequest;
import com.ququ.activity.dto.request.ActivityCreateRequest;
import com.ququ.activity.dto.request.ActivityQueryRequest;
import com.ququ.activity.dto.request.ActivitySummaryRequest;
import com.ququ.activity.dto.request.ActivityUpdateRequest;
import com.ququ.activity.dto.response.ActivityCheckinQrcodeResponse;
import com.ququ.activity.dto.response.ActivityCheckinResponse;
import com.ququ.activity.dto.response.ActivityCommentResponse;
import com.ququ.activity.dto.response.ActivityCreateResponse;
import com.ququ.activity.dto.response.ActivityListItemResponse;
import com.ququ.activity.dto.response.ActivityRegistrationItemResponse;
import com.ququ.activity.dto.response.ActivityRegistrationStatusResponse;
import com.ququ.activity.dto.response.ActivitySummaryResponse;
import com.ququ.activity.entity.Activity;
import com.ququ.activity.entity.ActivityReviewRecord;
import com.ququ.activity.mapper.ActivityMapper;
import com.ququ.activity.mapper.NotificationMapper;
import com.ququ.activity.mapper.ActivityReviewRecordMapper;
import com.ququ.activity.service.ActivityService;
import com.ququ.activity.service.UserClient;
import com.ququ.common.dto.PageResult;
import com.ququ.common.dto.UserBasicDTO;
import com.ququ.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
class ActivityServiceTest {
    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityReviewRecordMapper reviewRecordMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @MockBean
    private UserClient userClient;

    @Test
    void createDraftUpdateAndSubmitPublishesSmallActivity() {
        when(userClient.isUserValid(anyLong())).thenReturn(true);

        ActivityCreateResponse draft = activityService.create(10001L, request(true, 20, "周末徒步"));
        assertThat(draft.getStatus()).isEqualTo(0);

        ActivityUpdateRequest updateRequest = new ActivityUpdateRequest();
        copy(request(true, 20, "周末城市徒步"), updateRequest);
        ActivityCreateResponse updated = activityService.updateDraft(draft.getActivityId(), 10001L, updateRequest);
        assertThat(updated.getStatus()).isEqualTo(0);

        ActivityCreateResponse submitted = activityService.submit(draft.getActivityId(), 10001L);
        assertThat(submitted.getStatus()).isEqualTo(2);

        Activity saved = activityMapper.selectById(draft.getActivityId());
        assertThat(saved.getTitle()).isEqualTo("周末城市徒步");
        assertThat(saved.getReviewType()).isZero();

        ActivityReviewRecord record = latestReview(draft.getActivityId());
        assertThat(record.getAiResult()).isEqualTo("pass");
        assertThat(record.getFinalResult()).isZero();
    }

    @Test
    void largeActivityGoesToManualReview() {
        when(userClient.isUserValid(anyLong())).thenReturn(true);

        ActivityCreateResponse response = activityService.create(10001L, request(false, 80, "大型公益活动"));

        assertThat(response.getStatus()).isEqualTo(1);
        Activity saved = activityMapper.selectById(response.getActivityId());
        assertThat(saved.getReviewType()).isEqualTo(1);

        ActivityReviewRecord record = latestReview(response.getActivityId());
        assertThat(record.getAiResult()).isEqualTo("pass");
        assertThat(record.getFinalResult()).isEqualTo(3);
        assertThat(record.getReviewComment()).contains("Large activity");
    }

    @Test
    void listDetailAndTemplatesWork() {
        when(userClient.isUserValid(anyLong())).thenReturn(true);
        ActivityCreateResponse response = activityService.create(10001L, request(false, 20, "桌游聚会"));

        ActivityQueryRequest query = new ActivityQueryRequest();
        query.setKeyword("桌游");
        query.setStatus(2);

        assertThat(activityService.list(query).getList()).isNotEmpty();
        assertThat(activityService.detail(response.getActivityId()).getTitle()).isEqualTo("桌游聚会");
        assertThat(activityService.templates()).hasSizeGreaterThanOrEqualTo(6);
    }

    @Test
    void nonCreatorCannotUpdateDraft() {
        when(userClient.isUserValid(anyLong())).thenReturn(true);
        ActivityCreateResponse draft = activityService.create(10001L, request(true, 20, "学习交流"));

        ActivityUpdateRequest updateRequest = new ActivityUpdateRequest();
        copy(request(true, 20, "学习交流2"), updateRequest);

        assertThatThrownBy(() -> activityService.updateDraft(draft.getActivityId(), 20002L, updateRequest))
                .isInstanceOf(BizException.class);
    }

    @Test
    void registerWaitlistCancelPromoteAndConfirmWork() {
        when(userClient.isUserValid(anyLong())).thenReturn(true);
        when(userClient.getUserCredit(anyLong())).thenReturn(100);
        when(userClient.getUsersByIds(anyList())).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            return ids.stream()
                    .map(id -> new UserBasicDTO(id, "user-" + id, "/avatar/" + id + ".png", 0))
                    .toList();
        });
        ActivityCreateResponse created = activityService.create(10001L, request(false, 1, "小班桌游"));

        ActivityRegistrationStatusResponse first = activityService.register(created.getActivityId(), 20001L);
        assertThat(first.getRegistrationStatusText()).isEqualTo("registered");
        assertThat(first.getCurrentCount()).isEqualTo(1);

        assertThatThrownBy(() -> activityService.register(created.getActivityId(), 20001L))
                .isInstanceOf(BizException.class);

        ActivityRegistrationStatusResponse waiting = activityService.register(created.getActivityId(), 20002L);
        assertThat(waiting.getWaitlistStatusText()).isEqualTo("waiting");
        assertThat(waiting.getQueueNo()).isEqualTo(1);

        activityService.cancelRegistration(created.getActivityId(), 20001L);
        ActivityRegistrationStatusResponse pending = activityService.myRegistrationStatus(created.getActivityId(), 20002L);
        assertThat(pending.getWaitlistStatusText()).isEqualTo("pending_confirm");
        assertThat(notificationMapper.selectCount(null)).isEqualTo(1);

        ActivityRegistrationStatusResponse confirmed = activityService.register(created.getActivityId(), 20002L);
        assertThat(confirmed.getRegistrationStatusText()).isEqualTo("registered");
        assertThat(confirmed.getWaitlistStatus()).isNull();
        assertThat(confirmed.getCurrentCount()).isEqualTo(1);

        List<ActivityRegistrationItemResponse> registrations = activityService.registrations(created.getActivityId(), 10001L);
        assertThat(registrations).hasSize(1);
        assertThat(registrations.get(0).getUserId()).isEqualTo(20002L);
        assertThat(registrations.get(0).getNickname()).isEqualTo("user-20002");
    }

    @Test
    void checkinQrcodeLocationValidationAndDuplicateCheckinWork() {
        when(userClient.isUserValid(anyLong())).thenReturn(true);
        when(userClient.getUserCredit(anyLong())).thenReturn(100);

        ActivityCreateResponse created = activityService.create(10001L, request(false, 20, "城市徒步签到"));
        activityService.register(created.getActivityId(), 20001L);

        ActivityCheckinQrcodeResponse qrcode = activityService.generateCheckinQrcode(created.getActivityId(), 10001L);
        assertThat(qrcode.getQrcodeContent()).contains("\"activityId\":" + created.getActivityId());

        ActivityCheckinRequest checkinRequest = new ActivityCheckinRequest();
        checkinRequest.setQrcodeContent(qrcode.getQrcodeContent());
        checkinRequest.setLat(new BigDecimal("39.9289000"));
        checkinRequest.setLng(new BigDecimal("116.4833000"));

        ActivityCheckinResponse checkedIn = activityService.checkin(created.getActivityId(), 20001L, checkinRequest);
        assertThat(checkedIn.getCheckedIn()).isTrue();
        assertThat(checkedIn.getUserId()).isEqualTo(20001L);

        ActivityCheckinResponse duplicate = activityService.checkin(created.getActivityId(), 20001L, checkinRequest);
        assertThat(duplicate.getCheckinId()).isEqualTo(checkedIn.getCheckinId());

        ActivityCheckinRequest farAway = new ActivityCheckinRequest();
        farAway.setQrcodeContent(qrcode.getQrcodeContent());
        farAway.setLat(new BigDecimal("40.0000000"));
        farAway.setLng(new BigDecimal("116.5000000"));
        activityService.register(created.getActivityId(), 20002L);

        assertThatThrownBy(() -> activityService.checkin(created.getActivityId(), 20002L, farAway))
                .isInstanceOf(BizException.class);
    }

    @Test
    void publishSummaryAndCommentWork() {
        when(userClient.isUserValid(anyLong())).thenReturn(true);
        when(userClient.getUserCredit(anyLong())).thenReturn(100);
        when(userClient.getUsersByIds(anyList())).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            return ids.stream()
                    .map(id -> new UserBasicDTO(id, "user-" + id, "/avatar/" + id + ".png", 0))
                    .toList();
        });

        ActivityCreateResponse created = activityService.create(10001L, request(false, 20, "总结评价活动"));
        activityService.register(created.getActivityId(), 20001L);
        ActivityCheckinQrcodeResponse qrcode = activityService.generateCheckinQrcode(created.getActivityId(), 10001L);
        ActivityCheckinRequest checkinRequest = new ActivityCheckinRequest();
        checkinRequest.setQrcodeContent(qrcode.getQrcodeContent());
        checkinRequest.setLat(new BigDecimal("39.9289000"));
        checkinRequest.setLng(new BigDecimal("116.4833000"));
        activityService.checkin(created.getActivityId(), 20001L, checkinRequest);

        ActivitySummaryRequest summaryRequest = new ActivitySummaryRequest();
        summaryRequest.setTitle("活动圆满结束");
        summaryRequest.setContent("大家完成了城市徒步路线。");
        summaryRequest.setImageUrls(List.of("/uploads/activity-image/summary.jpg"));

        ActivitySummaryResponse summary = activityService.publishSummary(created.getActivityId(), 10001L, summaryRequest);
        assertThat(summary.getSummaryId()).isNotNull();
        assertThat(activityService.summary(created.getActivityId()).getTitle()).isEqualTo("活动圆满结束");

        ActivityCommentRequest commentRequest = new ActivityCommentRequest();
        commentRequest.setRating(5);
        commentRequest.setContent("体验很好，下次还来。");

        ActivityCommentResponse comment = activityService.publishComment(created.getActivityId(), 20001L, commentRequest);
        assertThat(comment.getCommentId()).isNotNull();
        assertThat(comment.getNickname()).isEqualTo("user-20001");
        assertThat(activityService.comments(created.getActivityId(), 1, 10).getList()).hasSize(1);

        ActivityCommentRequest updatedComment = new ActivityCommentRequest();
        updatedComment.setRating(4);
        updatedComment.setContent("更新一下评价。");
        ActivityCommentResponse updated = activityService.publishComment(created.getActivityId(), 20001L, updatedComment);
        assertThat(updated.getCommentId()).isEqualTo(comment.getCommentId());
        assertThat(activityService.comments(created.getActivityId(), 1, 10).getList()).hasSize(1);
        assertThat(activityService.comments(created.getActivityId(), 1, 10).getList().get(0).getRating()).isEqualTo(4);
    }

    @Test
    void transitionActivityStatusesMovesEligibleActivities() {
        LocalDateTime now = LocalDateTime.now();
        Activity published = activity(2, now.minusHours(1), now.plusHours(1), now.plusHours(3), "待报名状态流转");
        Activity registering = activity(3, now.plusHours(2), now.minusMinutes(1), now.plusHours(4), "待报名截止流转");
        Activity regClosed = activity(4, now.minusMinutes(1), now.minusHours(1), now.plusHours(2), "待进行中流转");
        Activity ongoing = activity(5, now.minusHours(3), now.minusHours(4), now.minusMinutes(1), "待结束流转");
        activityMapper.insert(published);
        activityMapper.insert(registering);
        activityMapper.insert(regClosed);
        activityMapper.insert(ongoing);

        int changed = activityService.transitionActivityStatuses();

        assertThat(changed).isEqualTo(4);
        assertThat(activityMapper.selectById(published.getId()).getStatus()).isEqualTo(3);
        assertThat(activityMapper.selectById(registering.getId()).getStatus()).isEqualTo(4);
        assertThat(activityMapper.selectById(regClosed.getId()).getStatus()).isEqualTo(5);
        assertThat(activityMapper.selectById(ongoing.getId()).getStatus()).isEqualTo(6);
    }

    @Test
    void nearbyQueryFiltersAndSortsByDistance() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(3);
        Activity near = activity(2, startTime, startTime.minusDays(1), startTime.plusHours(2), "附近活动");
        near.setLocationLat(new BigDecimal("39.9290000"));
        near.setLocationLng(new BigDecimal("116.4834000"));
        near.setTeamId(90001L);
        Activity farther = activity(2, startTime.plusHours(1), startTime.minusDays(1), startTime.plusHours(3), "较远活动");
        farther.setLocationLat(new BigDecimal("39.9350000"));
        farther.setLocationLng(new BigDecimal("116.4900000"));
        farther.setTeamId(90001L);
        Activity outside = activity(2, startTime.plusHours(2), startTime.minusDays(1), startTime.plusHours(4), "圈外活动");
        outside.setLocationLat(new BigDecimal("40.0000000"));
        outside.setLocationLng(new BigDecimal("116.5000000"));
        outside.setTeamId(90001L);
        activityMapper.insert(near);
        activityMapper.insert(farther);
        activityMapper.insert(outside);

        ActivityQueryRequest query = new ActivityQueryRequest();
        query.setTab("nearby");
        query.setLat(new BigDecimal("39.9289000"));
        query.setLng(new BigDecimal("116.4833000"));
        query.setRadius(1200);
        query.setTeamId(90001L);

        PageResult<ActivityListItemResponse> result = activityService.list(query);

        assertThat(result.getList()).extracting(ActivityListItemResponse::getActivityId)
                .containsExactly(near.getId(), farther.getId());
        assertThat(result.getList().get(0).getDistanceMeters()).isLessThan(result.getList().get(1).getDistanceMeters());
        assertThat(result.getList().get(0).getLocationLat()).isEqualByComparingTo("39.9290000");
        assertThat(result.getTotal()).isEqualTo(2);
    }

    private ActivityCreateRequest request(boolean draft, int maxParticipants, String title) {
        ActivityCreateRequest request = new ActivityCreateRequest();
        request.setTitle(title);
        request.setDescription("第一阶段活动测试");
        request.setTags(List.of("徒步", "户外"));
        request.setCoverUrl("https://example.com/cover.jpg");
        request.setStartTime(LocalDateTime.now().plusDays(3));
        request.setEndTime(LocalDateTime.now().plusDays(3).plusHours(3));
        request.setRegDeadline(LocalDateTime.now().plusDays(2));
        request.setLocationName("朝阳公园南门");
        request.setLocationLat(new BigDecimal("39.9289000"));
        request.setLocationLng(new BigDecimal("116.4833000"));
        request.setLocationDetail("北京市朝阳区朝阳公园南路1号");
        request.setMaxParticipants(maxParticipants);
        request.setFee(BigDecimal.ZERO);
        request.setLocationVerify(1);
        request.setLocationRadius(300);
        request.setIsDraft(draft);
        return request;
    }

    private Activity activity(Integer status,
                              LocalDateTime startTime,
                              LocalDateTime regDeadline,
                              LocalDateTime endTime,
                              String title) {
        Activity activity = new Activity();
        activity.setCreatorId(10001L);
        activity.setTitle(title);
        activity.setDescription("状态流转测试");
        activity.setTags("[\"测试\"]");
        activity.setStartTime(startTime);
        activity.setEndTime(endTime);
        activity.setRegDeadline(regDeadline);
        activity.setLocationName("测试地点");
        activity.setLocationLat(new BigDecimal("39.9289000"));
        activity.setLocationLng(new BigDecimal("116.4833000"));
        activity.setMaxParticipants(20);
        activity.setCurrentCount(0);
        activity.setFee(BigDecimal.ZERO);
        activity.setStatus(status);
        activity.setReviewType(0);
        activity.setIsTeamOnly(0);
        activity.setLocationVerify(0);
        activity.setLocationRadius(500);
        activity.setDeleted(0);
        return activity;
    }

    private void copy(ActivityCreateRequest source, ActivityUpdateRequest target) {
        target.setTitle(source.getTitle());
        target.setDescription(source.getDescription());
        target.setTags(source.getTags());
        target.setCoverUrl(source.getCoverUrl());
        target.setStartTime(source.getStartTime());
        target.setEndTime(source.getEndTime());
        target.setRegDeadline(source.getRegDeadline());
        target.setLocationName(source.getLocationName());
        target.setLocationLat(source.getLocationLat());
        target.setLocationLng(source.getLocationLng());
        target.setLocationDetail(source.getLocationDetail());
        target.setMaxParticipants(source.getMaxParticipants());
        target.setFee(source.getFee());
        target.setLocationVerify(source.getLocationVerify());
        target.setLocationRadius(source.getLocationRadius());
        target.setIsDraft(source.getIsDraft());
    }

    private ActivityReviewRecord latestReview(Long activityId) {
        return reviewRecordMapper.selectOne(new LambdaQueryWrapper<ActivityReviewRecord>()
                .eq(ActivityReviewRecord::getActivityId, activityId)
                .orderByDesc(ActivityReviewRecord::getId)
                .last("LIMIT 1"));
    }
}
