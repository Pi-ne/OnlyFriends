package com.onlyfriends.activity.controller.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.onlyfriends.activity.dto.request.AdminOfflineActivityRequest;
import com.onlyfriends.activity.dto.request.AdminReviewActivityRequest;
import com.onlyfriends.activity.dto.response.AdminActivityResponse;
import com.onlyfriends.activity.entity.Activity;
import com.onlyfriends.activity.entity.ActivityReviewRecord;
import com.onlyfriends.activity.mapper.ActivityMapper;
import com.onlyfriends.activity.mapper.ActivityReviewRecordMapper;
import com.onlyfriends.common.dto.PageResult;
import com.onlyfriends.common.exception.BizException;
import com.onlyfriends.common.response.Result;
import com.onlyfriends.common.response.ResultCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/admin/activities")
public class AdminActivityInternalController {
    private static final int STATUS_REVIEWING = 1;
    private static final int STATUS_PUBLISHED = 2;
    private static final int STATUS_REGISTERING = 3;
    private static final int STATUS_OFFLINE = 7;
    private static final int STATUS_REJECTED = 8;
    private static final int STATUS_NEEDS_MODIFICATION = 9;
    private static final int REVIEW_ACTION_PASS = 0;
    private static final int REVIEW_ACTION_REJECT = 1;
    private static final int REVIEW_ACTION_MODIFY = 2;

    private final ActivityMapper activityMapper;
    private final ActivityReviewRecordMapper reviewRecordMapper;

    @GetMapping("/pending")
    public Result<PageResult<AdminActivityResponse>> pendingActivities(@RequestParam(defaultValue = "1") Integer page,
                                                                       @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(pageActivities(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, STATUS_REVIEWING)
                .orderByDesc(Activity::getCreatedAt), page, size));
    }

    @GetMapping
    public Result<PageResult<AdminActivityResponse>> activities(@RequestParam(defaultValue = "1") Integer page,
                                                                @RequestParam(defaultValue = "20") Integer size,
                                                                @RequestParam(required = false) Integer status,
                                                                @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Activity::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Activity::getTitle, keyword);
        }
        wrapper.orderByDesc(Activity::getCreatedAt);
        return Result.success(pageActivities(wrapper, page, size));
    }

    @GetMapping("/{id}")
    public Result<AdminActivityResponse> activityDetail(@PathVariable Long id) {
        return Result.success(toResponse(getActivityOrThrow(id)));
    }

    @PutMapping("/{id}/review")
    @Transactional
    public Result<Void> review(@PathVariable Long id, @Valid @RequestBody AdminReviewActivityRequest request) {
        Activity activity = getActivityOrThrow(id);
        if (!Integer.valueOf(STATUS_REVIEWING).equals(activity.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "只有待审核活动可以审核");
        }
        if (!List.of(REVIEW_ACTION_PASS, REVIEW_ACTION_REJECT, REVIEW_ACTION_MODIFY).contains(request.getAction())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "审核结果不合法");
        }
        if ((Integer.valueOf(REVIEW_ACTION_REJECT).equals(request.getAction())
                || Integer.valueOf(REVIEW_ACTION_MODIFY).equals(request.getAction()))
                && !StringUtils.hasText(request.getComment())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "驳回或要求修改必须填写审核意见");
        }

        if (Integer.valueOf(REVIEW_ACTION_PASS).equals(request.getAction())) {
            activity.setStatus(STATUS_PUBLISHED);
        } else if (Integer.valueOf(REVIEW_ACTION_MODIFY).equals(request.getAction())) {
            activity.setStatus(STATUS_NEEDS_MODIFICATION);
        } else {
            activity.setStatus(STATUS_REJECTED);
        }
        activityMapper.updateById(activity);
        insertReviewRecord(id, request.getAdminId(), request.getAction(), request.getComment());
        return Result.success();
    }

    @PostMapping("/{id}/offline")
    @Transactional
    public Result<Void> offline(@PathVariable Long id, @Valid @RequestBody AdminOfflineActivityRequest request) {
        Activity activity = getActivityOrThrow(id);
        if (!List.of(STATUS_PUBLISHED, STATUS_REGISTERING).contains(activity.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "只有已发布活动可以下架");
        }
        activity.setStatus(STATUS_OFFLINE);
        activityMapper.updateById(activity);
        insertReviewRecord(id, request.getAdminId(), REVIEW_ACTION_REJECT, "下架：" + request.getReason().trim());
        return Result.success();
    }

    @PostMapping("/{id}/restore")
    @Transactional
    public Result<Void> restore(@PathVariable Long id) {
        Activity activity = getActivityOrThrow(id);
        if (!Integer.valueOf(STATUS_OFFLINE).equals(activity.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "只有已下架活动可以恢复");
        }
        activity.setStatus(STATUS_PUBLISHED);
        activityMapper.updateById(activity);
        return Result.success();
    }

    private PageResult<AdminActivityResponse> pageActivities(LambdaQueryWrapper<Activity> wrapper, Integer page, Integer size) {
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null ? 20 : Math.min(Math.max(size, 1), 100);
        List<Activity> all = activityMapper.selectList(wrapper);
        List<AdminActivityResponse> rows = all.stream()
                .skip((long) (current - 1) * pageSize)
                .limit(pageSize)
                .map(this::toResponse)
                .toList();
        return new PageResult<>(rows, (long) all.size(), (long) current, (long) pageSize);
    }

    private void insertReviewRecord(Long activityId, Long adminId, Integer result, String comment) {
        ActivityReviewRecord record = new ActivityReviewRecord();
        record.setActivityId(activityId);
        record.setReviewStage(1);
        record.setReviewerId(adminId);
        record.setAiRiskCategories("[]");
        record.setFinalResult(result);
        record.setReviewComment(comment);
        reviewRecordMapper.insert(record);
    }

    private Activity getActivityOrThrow(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "活动不存在");
        }
        return activity;
    }

    private AdminActivityResponse toResponse(Activity activity) {
        AdminActivityResponse response = new AdminActivityResponse();
        response.setActivityId(activity.getId());
        response.setCreatorId(activity.getCreatorId());
        response.setTitle(activity.getTitle());
        response.setStatus(activity.getStatus());
        response.setStatusText(statusText(activity.getStatus()));
        response.setReviewType(activity.getReviewType());
        response.setStartTime(activity.getStartTime());
        response.setRegDeadline(activity.getRegDeadline());
        response.setCreatedAt(activity.getCreatedAt());
        return response;
    }

    private String statusText(Integer status) {
        return switch (status == null ? -1 : status) {
            case 1 -> "reviewing";
            case 2 -> "published";
            case 3 -> "registering";
            case 7 -> "offline";
            case 8 -> "rejected";
            case 9 -> "needs_modify";
            default -> "unknown";
        };
    }
}
