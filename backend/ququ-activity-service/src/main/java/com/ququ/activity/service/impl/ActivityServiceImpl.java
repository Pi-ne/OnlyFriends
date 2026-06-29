package com.ququ.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ququ.activity.dto.ai.AiReviewRequest;
import com.ququ.activity.dto.ai.AiReviewResponse;
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
import com.ququ.activity.dto.response.ActivityDetailResponse;
import com.ququ.activity.dto.response.ActivityListItemResponse;
import com.ququ.activity.dto.response.ActivityRegistrationItemResponse;
import com.ququ.activity.dto.response.ActivityRegistrationStatusResponse;
import com.ququ.activity.dto.response.ActivitySummaryResponse;
import com.ququ.activity.dto.response.ActivityTagResponse;
import com.ququ.activity.dto.response.ActivityTemplateResponse;
import com.ququ.activity.dto.response.NotificationResponse;
import com.ququ.activity.entity.Activity;
import com.ququ.activity.entity.ActivityCheckin;
import com.ququ.activity.entity.ActivityComment;
import com.ququ.activity.entity.ActivityRegistration;
import com.ququ.activity.entity.ActivityReviewRecord;
import com.ququ.activity.entity.ActivitySummary;
import com.ququ.activity.entity.ActivityTag;
import com.ququ.activity.entity.ActivityTemplate;
import com.ququ.activity.entity.ActivityWaitlist;
import com.ququ.activity.entity.Notification;
import com.ququ.activity.mapper.ActivityCheckinMapper;
import com.ququ.activity.mapper.ActivityCommentMapper;
import com.ququ.activity.mapper.ActivityMapper;
import com.ququ.activity.mapper.ActivityRegistrationMapper;
import com.ququ.activity.mapper.ActivityReviewRecordMapper;
import com.ququ.activity.mapper.ActivitySummaryMapper;
import com.ququ.activity.mapper.ActivityTagMapper;
import com.ququ.activity.mapper.ActivityTemplateMapper;
import com.ququ.activity.mapper.ActivityWaitlistMapper;
import com.ququ.activity.mapper.NotificationMapper;
import com.ququ.activity.service.ActivityRedisSupport;
import com.ququ.activity.service.ActivityService;
import com.ququ.activity.service.SocialClient;
import com.ququ.activity.service.UserClient;
import com.ququ.activity.service.ai.AiClient;
import com.ququ.common.dto.UserBasicDTO;
import com.ququ.common.dto.PageResult;
import com.ququ.common.exception.BizException;
import com.ququ.common.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_REVIEWING = 1;
    private static final int STATUS_PUBLISHED = 2;
    private static final int STATUS_REGISTERING = 3;
    private static final int STATUS_REG_CLOSED = 4;
    private static final int STATUS_ONGOING = 5;
    private static final int STATUS_FINISHED = 6;
    private static final int STATUS_REJECTED = 8;
    private static final BigDecimal AUTO_PASS_CONFIDENCE = new BigDecimal("0.70");
    private static final BigDecimal AUTO_REJECT_CONFIDENCE = new BigDecimal("0.90");
    private static final int AUTO_PASS_MAX_PARTICIPANTS = 50;
    private static final int REVIEW_TYPE_AI = 0;
    private static final int REVIEW_TYPE_MANUAL = 1;
    private static final int REG_STATUS_REGISTERED = 1;
    private static final int REG_STATUS_CANCELLED = 2;
    private static final int WAIT_STATUS_WAITING = 0;
    private static final int WAIT_STATUS_PENDING = 1;
    private static final int WAIT_STATUS_CANCELLED = 2;
    private static final int WAIT_STATUS_EXPIRED = 3;
    private static final long ACTIVE_KEY = 0L;
    private static final int MIN_CREDIT = 60;
    private static final long CHECKIN_QR_TTL_SECONDS = 2 * 60 * 60;

    private final ActivityMapper activityMapper;
    private final ActivityTemplateMapper templateMapper;
    private final ActivityReviewRecordMapper reviewRecordMapper;
    private final ActivityRegistrationMapper registrationMapper;
    private final ActivityWaitlistMapper waitlistMapper;
    private final ActivityCheckinMapper checkinMapper;
    private final ActivitySummaryMapper summaryMapper;
    private final ActivityCommentMapper commentMapper;
    private final ActivityTagMapper tagMapper;
    private final NotificationMapper notificationMapper;
    private final ObjectMapper objectMapper;
    private final UserClient userClient;
    private final SocialClient socialClient;
    private final AiClient aiClient;
    private final ActivityRedisSupport redisSupport;

    @Value("${app.checkin.secret:change-me-checkin-secret-32bytes}")
    private String checkinSecret;

    @Value("${app.waitlist.pending-timeout-minutes:30}")
    private long waitlistPendingTimeoutMinutes;

    @Override
    @Transactional
    public ActivityCreateResponse create(Long creatorId, ActivityCreateRequest request) {
        ensureUserValid(creatorId);
        ensureTeamPublishAllowed(creatorId, request.getTeamId());
        validateTime(request.getStartTime(), request.getEndTime(), request.getRegDeadline());

        Activity activity = new Activity();
        fillActivity(activity, request);
        activity.setCreatorId(creatorId);
        activity.setCurrentCount(0);
        activity.setDeleted(0);
        activity.setStatus(Boolean.TRUE.equals(request.getIsDraft()) ? STATUS_DRAFT : STATUS_REVIEWING);
        activity.setReviewType(REVIEW_TYPE_AI);
        activity.setIsTeamOnly(request.getTeamId() == null ? 0 : 1);
        activityMapper.insert(activity);
        incrementTagUsage(request.getTags());

        if (!Boolean.TRUE.equals(request.getIsDraft())) {
            reviewActivityWithAi(activity);
        }
        return toCreateResponse(activity);
    }

    @Override
    @Transactional
    public ActivityCreateResponse updateDraft(Long activityId, Long creatorId, ActivityUpdateRequest request) {
        Activity activity = getActivityOrThrow(activityId);
        ensureCreator(activity, creatorId);
        if (!Integer.valueOf(STATUS_DRAFT).equals(activity.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "只有草稿活动可以编辑");
        }
        ensureTeamPublishAllowed(creatorId, request.getTeamId());
        validateTime(request.getStartTime(), request.getEndTime(), request.getRegDeadline());
        fillActivity(activity, request);
        activity.setIsTeamOnly(request.getTeamId() == null ? 0 : 1);
        activityMapper.updateById(activity);
        incrementTagUsage(request.getTags());
        return toCreateResponse(activity);
    }

    @Override
    @Transactional
    public ActivityCreateResponse submit(Long activityId, Long creatorId) {
        Activity activity = getActivityOrThrow(activityId);
        ensureCreator(activity, creatorId);
        if (!Integer.valueOf(STATUS_DRAFT).equals(activity.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "只有草稿活动可以提交审核");
        }
        validateTime(activity.getStartTime(), activity.getEndTime(), activity.getRegDeadline());
        activity.setStatus(STATUS_REVIEWING);
        activity.setReviewType(REVIEW_TYPE_AI);
        activityMapper.updateById(activity);
        reviewActivityWithAi(activity);
        return toCreateResponse(activity);
    }

    @Override
    @Transactional
    public ActivityCreateResponse cloneActivity(Long activityId, Long creatorId) {
        Activity source = getActivityOrThrow(activityId);
        ensureUserValid(creatorId);
        if (source.getTeamId() != null) {
            ensureTeamPublishAllowed(creatorId, source.getTeamId());
        }

        Activity cloned = new Activity();
        cloned.setCreatorId(creatorId);
        cloned.setTitle(source.getTitle() + " 副本");
        cloned.setDescription(source.getDescription());
        cloned.setTags(source.getTags());
        cloned.setCoverUrl(source.getCoverUrl());
        cloned.setStartTime(source.getStartTime());
        cloned.setEndTime(source.getEndTime());
        cloned.setRegDeadline(source.getRegDeadline());
        cloned.setLocationName(source.getLocationName());
        cloned.setLocationLat(source.getLocationLat());
        cloned.setLocationLng(source.getLocationLng());
        cloned.setLocationDetail(source.getLocationDetail());
        cloned.setMaxParticipants(source.getMaxParticipants());
        cloned.setCurrentCount(0);
        cloned.setFee(source.getFee());
        cloned.setStatus(STATUS_DRAFT);
        cloned.setReviewType(REVIEW_TYPE_AI);
        cloned.setIsTeamOnly(source.getTeamId() == null ? 0 : 1);
        cloned.setTeamId(source.getTeamId());
        cloned.setTemplateId(source.getTemplateId());
        cloned.setCloneFromId(source.getId());
        cloned.setLocationVerify(source.getLocationVerify());
        cloned.setLocationRadius(source.getLocationRadius());
        cloned.setDeleted(0);
        activityMapper.insert(cloned);
        return toCreateResponse(cloned);
    }

    @Override
    public PageResult<ActivityListItemResponse> list(ActivityQueryRequest request) {
        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int size = request.getSize() == null ? 20 : Math.min(Math.max(request.getSize(), 1), 50);

        if (Boolean.TRUE.equals(request.getRegistered())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "registered=true 查询请使用需要登录的 /activities/registered 接口");
        }

        LambdaQueryWrapper<Activity> wrapper = buildActivityQuery(request);
        if (isNearbyQuery(request)) {
            return nearbyList(request, wrapper, page, size);
        }

        applyListOrder(request, wrapper);

        IPage<Activity> resultPage = activityMapper.selectPage(new Page<>(page, size), wrapper);
        List<ActivityListItemResponse> list = resultPage.getRecords().stream().map(this::toListItem).toList();
        return new PageResult<>(list, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }

    @Override
    public PageResult<ActivityListItemResponse> registeredActivities(Long userId, Integer page, Integer size) {
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null ? 20 : Math.min(Math.max(size, 1), 50);
        List<Long> activityIds = registrationMapper.selectList(new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getUserId, userId)
                        .eq(ActivityRegistration::getStatus, REG_STATUS_REGISTERED)
                        .eq(ActivityRegistration::getActiveKey, ACTIVE_KEY)
                        .orderByDesc(ActivityRegistration::getRegisteredAt))
                .stream()
                .map(ActivityRegistration::getActivityId)
                .toList();
        if (activityIds.isEmpty()) {
            return new PageResult<>(List.of(), 0L, (long) current, (long) pageSize);
        }
        int fromIndex = Math.min((current - 1) * pageSize, activityIds.size());
        int toIndex = Math.min(fromIndex + pageSize, activityIds.size());
        List<Long> pageIds = activityIds.subList(fromIndex, toIndex);
        Map<Long, Activity> activityMap = activityMapper.selectBatchIds(pageIds).stream()
                .collect(Collectors.toMap(Activity::getId, Function.identity(), (left, right) -> left));
        List<ActivityListItemResponse> list = pageIds.stream()
                .map(activityMap::get)
                .filter(activity -> activity != null && !Integer.valueOf(1).equals(activity.getDeleted()))
                .map(this::toListItem)
                .toList();
        return new PageResult<>(list, (long) activityIds.size(), (long) current, (long) pageSize);
    }

    private LambdaQueryWrapper<Activity> buildActivityQuery(ActivityQueryRequest request) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getStatus() != null, Activity::getStatus, request.getStatus());
        if (request.getStatus() == null && isNearbyQuery(request)) {
            wrapper.in(Activity::getStatus, STATUS_PUBLISHED, STATUS_REGISTERING, STATUS_REG_CLOSED, STATUS_ONGOING);
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(Activity::getTitle, request.getKeyword())
                    .or()
                    .like(Activity::getDescription, request.getKeyword()));
        }
        wrapper.like(StringUtils.hasText(request.getCity()), Activity::getLocationDetail, request.getCity());
        wrapper.like(StringUtils.hasText(request.getLocationName()), Activity::getLocationName, request.getLocationName());
        wrapper.eq(request.getCreatorId() != null, Activity::getCreatorId, request.getCreatorId());
        wrapper.eq(request.getTeamId() != null, Activity::getTeamId, request.getTeamId());
        wrapper.ge(request.getMinFee() != null, Activity::getFee, request.getMinFee());
        wrapper.le(request.getMaxFee() != null, Activity::getFee, request.getMaxFee());
        wrapper.ge(request.getMinParticipants() != null, Activity::getMaxParticipants, request.getMinParticipants());
        wrapper.le(request.getMaxParticipants() != null, Activity::getMaxParticipants, request.getMaxParticipants());
        if (request.getStartDate() != null) {
            wrapper.ge(Activity::getStartTime, request.getStartDate().atStartOfDay());
        }
        if (request.getEndDate() != null) {
            wrapper.le(Activity::getStartTime, request.getEndDate().plusDays(1).atStartOfDay());
        }
        if (StringUtils.hasText(request.getTags())) {
            for (String tag : request.getTags().split(",")) {
                if (StringUtils.hasText(tag)) {
                    wrapper.like(Activity::getTags, tag.trim());
                }
            }
        }
        return wrapper;
    }

    private void applyListOrder(ActivityQueryRequest request, LambdaQueryWrapper<Activity> wrapper) {
        if ("recommend".equalsIgnoreCase(request.getTab())) {
            wrapper.orderByDesc(Activity::getCurrentCount).orderByAsc(Activity::getStartTime);
        } else if ("latest".equalsIgnoreCase(request.getTab())) {
            wrapper.orderByDesc(Activity::getCreatedAt);
        } else {
            wrapper.orderByDesc(Activity::getCreatedAt);
        }
    }

    private boolean isNearbyQuery(ActivityQueryRequest request) {
        return "nearby".equalsIgnoreCase(request.getTab()) || (request.getLat() != null && request.getLng() != null);
    }

    private PageResult<ActivityListItemResponse> nearbyList(ActivityQueryRequest request,
                                                            LambdaQueryWrapper<Activity> wrapper,
                                                            int page,
                                                            int size) {
        if (request.getLat() == null || request.getLng() == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "Nearby query requires lat and lng");
        }
        double lat = request.getLat().doubleValue();
        double lng = request.getLng().doubleValue();
        int radius = request.getRadius() == null || request.getRadius() <= 0 ? 5000 : Math.min(request.getRadius(), 100000);

        double latDelta = radius / 111320D;
        double lngDelta = radius / (111320D * Math.max(Math.cos(Math.toRadians(lat)), 0.01D));
        wrapper.isNotNull(Activity::getLocationLat)
                .isNotNull(Activity::getLocationLng)
                .ge(Activity::getLocationLat, BigDecimal.valueOf(lat - latDelta))
                .le(Activity::getLocationLat, BigDecimal.valueOf(lat + latDelta))
                .ge(Activity::getLocationLng, BigDecimal.valueOf(lng - lngDelta))
                .le(Activity::getLocationLng, BigDecimal.valueOf(lng + lngDelta));

        List<ActivityListItemResponse> nearby = activityMapper.selectList(wrapper).stream()
                .map(activity -> toNearbyListItem(activity, lat, lng))
                .filter(item -> item.getDistanceMeters() != null && item.getDistanceMeters() <= radius)
                .sorted(Comparator.comparing(ActivityListItemResponse::getDistanceMeters)
                        .thenComparing(ActivityListItemResponse::getStartTime))
                .toList();
        int fromIndex = Math.min((page - 1) * size, nearby.size());
        int toIndex = Math.min(fromIndex + size, nearby.size());
        return new PageResult<>(nearby.subList(fromIndex, toIndex), (long) nearby.size(), (long) page, (long) size);
    }

    @Override
    public ActivityDetailResponse detail(Long activityId) {
        return toDetail(getActivityOrThrow(activityId));
    }

    @Override
    public List<ActivityTemplateResponse> templates() {
        return templateMapper.selectList(new LambdaQueryWrapper<ActivityTemplate>().orderByDesc(ActivityTemplate::getSortOrder)).stream()
                .map(this::toTemplateResponse)
                .toList();
    }

    @Override
    public List<ActivityTagResponse> tags(String category, Integer limit) {
        int max = limit == null ? 50 : Math.min(Math.max(limit, 1), 100);
        return tagMapper.selectList(new LambdaQueryWrapper<ActivityTag>()
                        .eq(StringUtils.hasText(category), ActivityTag::getCategory, category)
                        .orderByDesc(ActivityTag::getUsageCount)
                        .orderByAsc(ActivityTag::getSortOrder)
                        .last("LIMIT " + max))
                .stream()
                .map(this::toTagResponse)
                .toList();
    }

    @Override
    @Transactional
    public ActivityRegistrationStatusResponse register(Long activityId, Long userId) {
        Activity activity = getActivityOrThrow(activityId);
        validateRegisterable(activity);
        ensureNotCreator(activity, userId);
        ensureRegistrantValid(userId);

        if (activeRegistration(activityId, userId) != null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "用户不能重复报名");
        }
        ActivityWaitlist currentWaitlist = activeWaitlist(activityId, userId);
        if (currentWaitlist != null) {
            if (Integer.valueOf(WAIT_STATUS_PENDING).equals(currentWaitlist.getStatus())) {
                return confirmPendingWaitlist(activityId, userId, currentWaitlist);
            }
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "用户已在等待队列中");
        }

        if (!hasPendingWaitlist(activityId) && activityMapper.tryOccupySeat(activityId) > 0) {
            ActivityRegistration registration = new ActivityRegistration();
            registration.setActivityId(activityId);
            registration.setUserId(userId);
            registration.setStatus(REG_STATUS_REGISTERED);
            registration.setActiveKey(ACTIVE_KEY);
            registration.setRegisteredAt(LocalDateTime.now());
            try {
                registrationMapper.insert(registration);
            } catch (DuplicateKeyException ex) {
                activityMapper.releaseSeat(activityId);
                throw new BizException(ResultCode.BAD_REQUEST.getCode(), "用户不能重复报名");
            }
            return myRegistrationStatus(activityId, userId);
        }

        ActivityWaitlist waitlist = new ActivityWaitlist();
        waitlist.setActivityId(activityId);
        waitlist.setUserId(userId);
        waitlist.setQueueNo(waitlistMapper.nextQueueNo(activityId));
        waitlist.setStatus(WAIT_STATUS_WAITING);
        waitlist.setActiveKey(ACTIVE_KEY);
        try {
            waitlistMapper.insert(waitlist);
        } catch (DuplicateKeyException ex) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "用户不能重复报名或排队");
        }
        redisSupport.addWaitlist(waitlist);
        return myRegistrationStatus(activityId, userId);
    }

    @Override
    @Transactional
    public ActivityRegistrationStatusResponse cancelRegistration(Long activityId, Long userId) {
        Activity activity = getActivityOrThrow(activityId);
        if (!LocalDateTime.now().isBefore(activity.getRegDeadline())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "报名截止后不能取消报名");
        }

        ActivityRegistration registration = activeRegistration(activityId, userId);
        if (registration != null) {
            registration.setStatus(REG_STATUS_CANCELLED);
            registration.setCancelledAt(LocalDateTime.now());
            registration.setActiveKey(registration.getId());
            registrationMapper.updateById(registration);
            activityMapper.releaseSeat(activityId);
            redisSupport.withPromotionLock(activityId, () -> promoteFirstWaiting(activityId));
            return myRegistrationStatus(activityId, userId);
        }

        ActivityWaitlist waitlist = activeWaitlist(activityId, userId);
        if (waitlist == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "未找到当前报名或等待记录");
        }
        waitlist.setStatus(WAIT_STATUS_CANCELLED);
        waitlist.setCancelledAt(LocalDateTime.now());
        waitlist.setActiveKey(waitlist.getId());
        waitlistMapper.updateById(waitlist);
        redisSupport.removeWaitlist(activityId, userId);
        return myRegistrationStatus(activityId, userId);
    }

    @Override
    public ActivityRegistrationStatusResponse myRegistrationStatus(Long activityId, Long userId) {
        Activity activity = getActivityOrThrow(activityId);
        ActivityRegistration registration = activeRegistration(activityId, userId);
        ActivityWaitlist waitlist = activeWaitlist(activityId, userId);
        return toRegistrationStatus(activity, userId, registration, waitlist);
    }

    @Override
    public List<ActivityRegistrationItemResponse> registrations(Long activityId, Long creatorId) {
        Activity activity = getActivityOrThrow(activityId);
        ensureCreator(activity, creatorId);
        List<ActivityRegistration> registrations = registrationMapper.selectList(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getStatus, REG_STATUS_REGISTERED)
                .eq(ActivityRegistration::getActiveKey, ACTIVE_KEY)
                .orderByAsc(ActivityRegistration::getRegisteredAt));
        Map<Long, UserBasicDTO> users = userClient.getUsersByIds(registrations.stream()
                        .map(ActivityRegistration::getUserId)
                        .distinct()
                        .toList())
                .stream()
                .filter(user -> user.getUserId() != null)
                .collect(Collectors.toMap(UserBasicDTO::getUserId, Function.identity(), (left, right) -> left));
        return registrations.stream()
                .map(registration -> toRegistrationItem(registration, users.get(registration.getUserId())))
                .toList();
    }

    @Override
    @Transactional
    public ActivityCheckinQrcodeResponse generateCheckinQrcode(Long activityId, Long creatorId) {
        Activity activity = getActivityOrThrow(activityId);
        ensureCreator(activity, creatorId);
        if (!Set.of(STATUS_PUBLISHED, STATUS_REGISTERING, 4, 5).contains(activity.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "当前活动状态不能生成签到码");
        }
        LocalDateTime generatedAt = LocalDateTime.now();
        LocalDateTime expiresAt = generatedAt.plusSeconds(CHECKIN_QR_TTL_SECONDS);
        long timestamp = generatedAt.toEpochSecond(ZoneOffset.ofHours(8));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("activityId", activityId);
        payload.put("timestamp", timestamp);
        payload.put("sign", sign(activityId, timestamp));
        String qrcodeContent = toJsonObject(payload);
        activity.setCheckinQrCode(qrcodeContent);
        activityMapper.updateById(activity);

        ActivityCheckinQrcodeResponse response = new ActivityCheckinQrcodeResponse();
        response.setActivityId(activityId);
        response.setQrcodeContent(qrcodeContent);
        response.setGeneratedAt(generatedAt);
        response.setExpiresAt(expiresAt);
        return response;
    }

    @Override
    @Transactional
    public ActivityCheckinResponse checkin(Long activityId, Long userId, ActivityCheckinRequest request) {
        Activity activity = getActivityOrThrow(activityId);
        validateCheckinPayload(activityId, request.getQrcodeContent());
        if (activeRegistration(activityId, userId) == null) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "只有已报名用户可以签到");
        }
        ActivityCheckin existing = checkinMapper.selectOne(new LambdaQueryWrapper<ActivityCheckin>()
                .eq(ActivityCheckin::getActivityId, activityId)
                .eq(ActivityCheckin::getUserId, userId)
                .last("LIMIT 1"));
        if (existing != null) {
            return toCheckinResponse(existing);
        }
        if (Integer.valueOf(1).equals(activity.getLocationVerify())) {
            validateCheckinLocation(activity, request);
        }
        ActivityCheckin checkin = new ActivityCheckin();
        checkin.setActivityId(activityId);
        checkin.setUserId(userId);
        checkin.setCheckinLat(request.getLat());
        checkin.setCheckinLng(request.getLng());
        checkin.setCheckinTime(LocalDateTime.now());
        try {
            checkinMapper.insert(checkin);
        } catch (DuplicateKeyException ex) {
            ActivityCheckin saved = checkinMapper.selectOne(new LambdaQueryWrapper<ActivityCheckin>()
                    .eq(ActivityCheckin::getActivityId, activityId)
                    .eq(ActivityCheckin::getUserId, userId)
                    .last("LIMIT 1"));
            return toCheckinResponse(saved);
        }
        return toCheckinResponse(checkin);
    }

    @Override
    @Transactional
    public ActivitySummaryResponse publishSummary(Long activityId, Long creatorId, ActivitySummaryRequest request) {
        Activity activity = getActivityOrThrow(activityId);
        ensureCreator(activity, creatorId);
        ActivitySummary summary = summaryMapper.selectOne(new LambdaQueryWrapper<ActivitySummary>()
                .eq(ActivitySummary::getActivityId, activityId)
                .last("LIMIT 1"));
        if (summary == null) {
            summary = new ActivitySummary();
            summary.setActivityId(activityId);
            summary.setCreatorId(creatorId);
        }
        summary.setTitle(request.getTitle().trim());
        summary.setContent(request.getContent().trim());
        summary.setImageUrls(toJson(request.getImageUrls()));
        if (summary.getId() == null) {
            summaryMapper.insert(summary);
        } else {
            summaryMapper.updateById(summary);
        }
        return toSummaryResponse(summary);
    }

    @Override
    public ActivitySummaryResponse summary(Long activityId) {
        getActivityOrThrow(activityId);
        ActivitySummary summary = summaryMapper.selectOne(new LambdaQueryWrapper<ActivitySummary>()
                .eq(ActivitySummary::getActivityId, activityId)
                .last("LIMIT 1"));
        if (summary == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "活动总结不存在");
        }
        return toSummaryResponse(summary);
    }

    @Override
    @Transactional
    public ActivityCommentResponse publishComment(Long activityId, Long userId, ActivityCommentRequest request) {
        getActivityOrThrow(activityId);
        if (activeRegistration(activityId, userId) == null) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "只有已报名用户可以评价活动");
        }
        ActivityCheckin checkin = checkinMapper.selectOne(new LambdaQueryWrapper<ActivityCheckin>()
                .eq(ActivityCheckin::getActivityId, activityId)
                .eq(ActivityCheckin::getUserId, userId)
                .last("LIMIT 1"));
        if (checkin == null) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "签到后才能评价活动");
        }
        ActivityComment comment = commentMapper.selectOne(new LambdaQueryWrapper<ActivityComment>()
                .eq(ActivityComment::getActivityId, activityId)
                .eq(ActivityComment::getUserId, userId)
                .last("LIMIT 1"));
        if (comment == null) {
            comment = new ActivityComment();
            comment.setActivityId(activityId);
            comment.setUserId(userId);
        }
        comment.setRating(request.getRating());
        comment.setContent(request.getContent().trim());
        if (comment.getId() == null) {
            commentMapper.insert(comment);
        } else {
            commentMapper.updateById(comment);
        }
        return toCommentResponse(comment, userMap(List.of(userId)).get(userId));
    }

    @Override
    public PageResult<ActivityCommentResponse> comments(Long activityId, Integer page, Integer size) {
        getActivityOrThrow(activityId);
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null ? 10 : Math.min(Math.max(size, 1), 50);
        IPage<ActivityComment> resultPage = commentMapper.selectPage(new Page<>(current, pageSize),
                new LambdaQueryWrapper<ActivityComment>()
                        .eq(ActivityComment::getActivityId, activityId)
                        .orderByDesc(ActivityComment::getCreatedAt));
        Map<Long, UserBasicDTO> users = userMap(resultPage.getRecords().stream()
                .map(ActivityComment::getUserId)
                .distinct()
                .toList());
        List<ActivityCommentResponse> list = resultPage.getRecords().stream()
                .map(comment -> toCommentResponse(comment, users.get(comment.getUserId())))
                .toList();
        return new PageResult<>(list, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }

    @Override
    public PageResult<NotificationResponse> notifications(Long userId, Integer page, Integer size, Boolean unreadOnly) {
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null ? 20 : Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Boolean.TRUE.equals(unreadOnly), Notification::getReadFlag, 0)
                .orderByDesc(Notification::getCreatedAt)
                .orderByDesc(Notification::getId);
        IPage<Notification> resultPage = notificationMapper.selectPage(new Page<>(current, pageSize), wrapper);
        List<NotificationResponse> list = resultPage.getRecords().stream().map(this::toNotificationResponse).toList();
        return new PageResult<>(list, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize());
    }

    @Override
    @Transactional
    public void markNotificationRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !userId.equals(notification.getUserId())) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "通知不存在");
        }
        if (!Integer.valueOf(1).equals(notification.getReadFlag())) {
            notification.setReadFlag(1);
            notificationMapper.updateById(notification);
        }
    }

    @Override
    @Transactional
    public int transitionActivityStatuses() {
        LocalDateTime now = LocalDateTime.now();
        int changed = 0;
        changed += transitionStatus(STATUS_PUBLISHED, STATUS_REGISTERING, Activity::getStartTime, now, true);
        changed += transitionStatus(STATUS_REGISTERING, STATUS_REG_CLOSED, Activity::getRegDeadline, now, true);
        changed += transitionStatus(STATUS_REG_CLOSED, STATUS_ONGOING, Activity::getStartTime, now, true);
        changed += transitionStatus(STATUS_ONGOING, STATUS_FINISHED, Activity::getEndTime, now, true);
        changed += expirePendingWaitlists();
        return changed;
    }

    @Override
    @Transactional
    public int expirePendingWaitlists() {
        LocalDateTime expireBefore = LocalDateTime.now().minusMinutes(Math.max(1, waitlistPendingTimeoutMinutes));
        List<ActivityWaitlist> expired = waitlistMapper.selectList(new LambdaQueryWrapper<ActivityWaitlist>()
                .eq(ActivityWaitlist::getStatus, WAIT_STATUS_PENDING)
                .eq(ActivityWaitlist::getActiveKey, ACTIVE_KEY)
                .le(ActivityWaitlist::getPendingAt, expireBefore));
        int changed = 0;
        for (ActivityWaitlist waitlist : expired) {
            waitlist.setStatus(WAIT_STATUS_EXPIRED);
            waitlist.setCancelledAt(LocalDateTime.now());
            waitlist.setActiveKey(waitlist.getId());
            waitlistMapper.updateById(waitlist);
            notifyWaitlistExpired(waitlist);
            redisSupport.withPromotionLock(waitlist.getActivityId(), () -> promoteFirstWaiting(waitlist.getActivityId()));
            changed++;
        }
        return changed;
    }

    private int transitionStatus(Integer fromStatus,
                                 Integer toStatus,
                                 Function<Activity, LocalDateTime> timeGetter,
                                 LocalDateTime now,
                                 boolean inclusive) {
        List<Activity> activities = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, fromStatus)
                .eq(Activity::getDeleted, 0));
        int changed = 0;
        for (Activity activity : activities) {
            LocalDateTime boundary = timeGetter.apply(activity);
            if (boundary != null && (inclusive ? !boundary.isAfter(now) : boundary.isBefore(now))) {
                activity.setStatus(toStatus);
                activityMapper.updateById(activity);
                changed++;
            }
        }
        return changed;
    }

    private void fillActivity(Activity activity, ActivityCreateRequest request) {
        activity.setTitle(request.getTitle().trim());
        activity.setDescription(request.getDescription());
        activity.setTags(toJson(request.getTags()));
        activity.setCoverUrl(request.getCoverUrl());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setRegDeadline(request.getRegDeadline());
        activity.setLocationName(request.getLocationName().trim());
        activity.setLocationLat(request.getLocationLat());
        activity.setLocationLng(request.getLocationLng());
        activity.setLocationDetail(request.getLocationDetail());
        activity.setMaxParticipants(request.getMaxParticipants());
        activity.setFee(request.getFee() == null ? BigDecimal.ZERO : request.getFee());
        activity.setLocationVerify(request.getLocationVerify() == null ? 0 : request.getLocationVerify());
        activity.setLocationRadius(request.getLocationRadius() == null ? 500 : request.getLocationRadius());
        activity.setTemplateId(request.getTemplateId());
        activity.setTeamId(request.getTeamId());
    }

    private ActivityRegistrationStatusResponse confirmPendingWaitlist(Long activityId, Long userId, ActivityWaitlist waitlist) {
        if (activityMapper.tryOccupySeat(activityId) <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "名额已满，请继续等待");
        }
        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityId(activityId);
        registration.setUserId(userId);
        registration.setStatus(REG_STATUS_REGISTERED);
        registration.setActiveKey(ACTIVE_KEY);
        registration.setRegisteredAt(LocalDateTime.now());
        try {
            registrationMapper.insert(registration);
        } catch (DuplicateKeyException ex) {
            activityMapper.releaseSeat(activityId);
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "用户不能重复报名");
        }
        waitlist.setStatus(WAIT_STATUS_CANCELLED);
        waitlist.setCancelledAt(LocalDateTime.now());
        waitlist.setActiveKey(waitlist.getId());
        waitlistMapper.updateById(waitlist);
        redisSupport.removeWaitlist(activityId, userId);
        return myRegistrationStatus(activityId, userId);
    }

    private void promoteFirstWaiting(Long activityId) {
        ActivityWaitlist first = waitlistMapper.selectList(new LambdaQueryWrapper<ActivityWaitlist>()
                        .eq(ActivityWaitlist::getActivityId, activityId)
                        .eq(ActivityWaitlist::getStatus, WAIT_STATUS_WAITING)
                        .eq(ActivityWaitlist::getActiveKey, ACTIVE_KEY)
                        .orderByAsc(ActivityWaitlist::getQueueNo)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
        if (first == null) {
            return;
        }
        first.setStatus(WAIT_STATUS_PENDING);
        first.setPendingAt(LocalDateTime.now());
        waitlistMapper.updateById(first);
        redisSupport.removeWaitlist(activityId, first.getUserId());
        mockNotifyWaitlistPending(first);
        redisSupport.publishWaitlistPending(first);
    }

    private void mockNotifyWaitlistPending(ActivityWaitlist waitlist) {
        Notification notification = new Notification();
        notification.setUserId(waitlist.getUserId());
        notification.setType("WAITLIST_PENDING");
        notification.setTitle("活动候补名额待确认");
        notification.setContent("你等待的活动已释放名额，请尽快确认报名。");
        notification.setRelatedType("activity");
        notification.setRelatedId(waitlist.getActivityId());
        notification.setReadFlag(0);
        notificationMapper.insert(notification);
    }

    private void notifyWaitlistExpired(ActivityWaitlist waitlist) {
        Notification notification = new Notification();
        notification.setUserId(waitlist.getUserId());
        notification.setType("WAITLIST_EXPIRED");
        notification.setTitle("活动候补名额已过期");
        notification.setContent("你等待的活动候补名额已超过确认时间，已自动让给下一位候补用户。");
        notification.setRelatedType("activity");
        notification.setRelatedId(waitlist.getActivityId());
        notification.setReadFlag(0);
        notificationMapper.insert(notification);
    }

    private void validateRegisterable(Activity activity) {
        if (!Set.of(STATUS_PUBLISHED, STATUS_REGISTERING).contains(activity.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "当前活动状态不允许报名");
        }
        if (!LocalDateTime.now().isBefore(activity.getRegDeadline())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "已超过报名截止时间");
        }
    }

    private void validateCheckinPayload(Long expectedActivityId, String qrcodeContent) {
        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(qrcodeContent, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "签到二维码内容格式错误");
        }
        Long activityId = numberAsLong(payload.get("activityId"));
        Long timestamp = numberAsLong(payload.get("timestamp"));
        String signed = payload.get("sign") == null ? null : String.valueOf(payload.get("sign"));
        if (!expectedActivityId.equals(activityId) || timestamp == null || !StringUtils.hasText(signed)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "签到二维码内容无效");
        }
        long now = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(8));
        if (now - timestamp > CHECKIN_QR_TTL_SECONDS || timestamp - now > 60) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "签到二维码已过期");
        }
        if (!sign(expectedActivityId, timestamp).equals(signed)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "签到二维码签名无效");
        }
    }

    private Long numberAsLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void validateCheckinLocation(Activity activity, ActivityCheckinRequest request) {
        if (request.getLat() == null || request.getLng() == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "该活动需要提交签到位置");
        }
        if (activity.getLocationLat() == null || activity.getLocationLng() == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "活动未配置签到位置");
        }
        double distance = distanceMeters(
                activity.getLocationLat().doubleValue(),
                activity.getLocationLng().doubleValue(),
                request.getLat().doubleValue(),
                request.getLng().doubleValue());
        int radius = activity.getLocationRadius() == null ? 500 : activity.getLocationRadius();
        if (distance > radius) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "当前位置超出签到范围");
        }
    }

    private double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000D;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String sign(Long activityId, long timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(checkinSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((activityId + ":" + timestamp).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new BizException(ResultCode.INTERNAL_ERROR.getCode(), "签到签名生成失败");
        }
    }

    private void ensureRegistrantValid(Long userId) {
        if (!userClient.isUserValid(userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "用户不存在或状态异常");
        }
        if (userClient.getUserCredit(userId) < MIN_CREDIT) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "用户信誉分不足，不能报名活动");
        }
    }

    private ActivityRegistration activeRegistration(Long activityId, Long userId) {
        return registrationMapper.selectOne(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId)
                .eq(ActivityRegistration::getStatus, REG_STATUS_REGISTERED)
                .eq(ActivityRegistration::getActiveKey, ACTIVE_KEY)
                .last("LIMIT 1"));
    }

    private ActivityWaitlist activeWaitlist(Long activityId, Long userId) {
        return waitlistMapper.selectOne(new LambdaQueryWrapper<ActivityWaitlist>()
                .eq(ActivityWaitlist::getActivityId, activityId)
                .eq(ActivityWaitlist::getUserId, userId)
                .in(ActivityWaitlist::getStatus, WAIT_STATUS_WAITING, WAIT_STATUS_PENDING)
                .eq(ActivityWaitlist::getActiveKey, ACTIVE_KEY)
                .last("LIMIT 1"));
    }

    private boolean hasPendingWaitlist(Long activityId) {
        return waitlistMapper.selectCount(new LambdaQueryWrapper<ActivityWaitlist>()
                .eq(ActivityWaitlist::getActivityId, activityId)
                .eq(ActivityWaitlist::getStatus, WAIT_STATUS_PENDING)
                .eq(ActivityWaitlist::getActiveKey, ACTIVE_KEY)) > 0;
    }

    private ActivityRegistrationStatusResponse toRegistrationStatus(Activity activity,
                                                                    Long userId,
                                                                    ActivityRegistration registration,
                                                                    ActivityWaitlist waitlist) {
        ActivityRegistrationStatusResponse response = new ActivityRegistrationStatusResponse();
        response.setActivityId(activity.getId());
        response.setUserId(userId);
        response.setCurrentCount(activity.getCurrentCount());
        response.setMaxParticipants(activity.getMaxParticipants());
        if (registration != null) {
            response.setRegistrationStatus(registration.getStatus());
            response.setRegistrationStatusText(registrationStatusText(registration.getStatus()));
        }
        if (waitlist != null) {
            response.setWaitlistStatus(waitlist.getStatus());
            response.setWaitlistStatusText(waitlistStatusText(waitlist.getStatus()));
            response.setQueueNo(waitlist.getQueueNo());
        }
        return response;
    }

    private ActivityRegistrationItemResponse toRegistrationItem(ActivityRegistration registration, UserBasicDTO user) {
        ActivityRegistrationItemResponse response = new ActivityRegistrationItemResponse();
        response.setRegistrationId(registration.getId());
        response.setUserId(registration.getUserId());
        if (user != null) {
            response.setNickname(user.getNickname());
            response.setAvatarUrl(user.getAvatarUrl());
            response.setUserType(user.getUserType());
        }
        response.setStatus(registration.getStatus());
        response.setStatusText(registrationStatusText(registration.getStatus()));
        response.setRegisteredAt(registration.getRegisteredAt());
        return response;
    }

    private ActivityCheckinResponse toCheckinResponse(ActivityCheckin checkin) {
        ActivityCheckinResponse response = new ActivityCheckinResponse();
        response.setActivityId(checkin.getActivityId());
        response.setUserId(checkin.getUserId());
        response.setCheckinId(checkin.getId());
        response.setCheckedIn(true);
        response.setCheckinTime(checkin.getCheckinTime());
        return response;
    }

    private ActivitySummaryResponse toSummaryResponse(ActivitySummary summary) {
        ActivitySummaryResponse response = new ActivitySummaryResponse();
        response.setSummaryId(summary.getId());
        response.setActivityId(summary.getActivityId());
        response.setCreatorId(summary.getCreatorId());
        response.setTitle(summary.getTitle());
        response.setContent(summary.getContent());
        response.setImageUrls(fromJson(summary.getImageUrls()));
        response.setCreatedAt(summary.getCreatedAt());
        return response;
    }

    private ActivityCommentResponse toCommentResponse(ActivityComment comment, UserBasicDTO user) {
        ActivityCommentResponse response = new ActivityCommentResponse();
        response.setCommentId(comment.getId());
        response.setActivityId(comment.getActivityId());
        response.setUserId(comment.getUserId());
        if (user != null) {
            response.setNickname(user.getNickname());
            response.setAvatarUrl(user.getAvatarUrl());
        }
        response.setRating(comment.getRating());
        response.setContent(comment.getContent());
        response.setCreatedAt(comment.getCreatedAt());
        return response;
    }

    private Map<Long, UserBasicDTO> userMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userClient.getUsersByIds(userIds).stream()
                .filter(user -> user.getUserId() != null)
                .collect(Collectors.toMap(UserBasicDTO::getUserId, Function.identity(), (left, right) -> left));
    }

    private void reviewActivityWithAi(Activity activity) {
        AiReviewRequest request = new AiReviewRequest();
        request.setActivityId(activity.getId());
        request.setTitle(activity.getTitle());
        request.setDescription(activity.getDescription());
        request.setTags(fromJson(activity.getTags()));
        request.setMaxParticipants(activity.getMaxParticipants());

        ActivityReviewRecord record = new ActivityReviewRecord();
        record.setActivityId(activity.getId());
        record.setReviewStage(0);
        try {
            AiReviewResponse review = aiClient.reviewContent(request);
            applyAiReviewResult(activity, record, review);
        } catch (Exception ex) {
            activity.setStatus(STATUS_REVIEWING);
            activity.setReviewType(REVIEW_TYPE_MANUAL);
            record.setAiResult("error");
            record.setAiRiskLevel(5);
            record.setAiRiskCategories(toJson(List.of("ai_unavailable")));
            record.setAiReason("AI review failed or timed out: " + ex.getMessage());
            record.setAiConfidence(BigDecimal.ZERO);
            record.setFinalResult(3);
            record.setReviewComment("AI unavailable, transferred to manual review");
        }
        activityMapper.updateById(activity);
        reviewRecordMapper.insert(record);
    }

    private void applyAiReviewResult(Activity activity, ActivityReviewRecord record, AiReviewResponse review) {
        BigDecimal confidence = review.getConfidence() == null ? BigDecimal.ZERO : review.getConfidence();
        String result = review.getResult() == null ? "risk" : review.getResult();
        boolean overAutoPassLimit = activity.getMaxParticipants() != null
                && activity.getMaxParticipants() > AUTO_PASS_MAX_PARTICIPANTS;

        record.setAiResult(result);
        record.setAiRiskLevel(review.getRiskLevel());
        record.setAiRiskCategories(toJson(review.getRiskCategories()));
        record.setAiReason(review.getReason());
        record.setAiConfidence(confidence);

        if ("pass".equals(result) && confidence.compareTo(AUTO_PASS_CONFIDENCE) >= 0 && !overAutoPassLimit) {
            activity.setStatus(STATUS_PUBLISHED);
            activity.setReviewType(REVIEW_TYPE_AI);
            record.setFinalResult(0);
            record.setReviewComment("AI auto pass");
        } else if ("reject".equals(result) && confidence.compareTo(AUTO_REJECT_CONFIDENCE) >= 0) {
            activity.setStatus(STATUS_REJECTED);
            activity.setReviewType(REVIEW_TYPE_AI);
            record.setFinalResult(1);
            record.setReviewComment("AI auto reject");
        } else {
            activity.setStatus(STATUS_REVIEWING);
            activity.setReviewType(REVIEW_TYPE_MANUAL);
            record.setFinalResult(3);
            record.setReviewComment(overAutoPassLimit ? "Large activity, transferred to manual review" : "AI risk or low confidence, transferred to manual review");
        }
    }

    private void validateTime(LocalDateTime startTime, LocalDateTime endTime, LocalDateTime regDeadline) {
        if (!endTime.isAfter(startTime)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "结束时间必须晚于开始时间");
        }
        if (!regDeadline.isBefore(startTime)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "报名截止时间必须早于开始时间");
        }
    }

    private void ensureUserValid(Long userId) {
        if (!userClient.isUserValid(userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "创建者不存在或状态异常");
        }
    }

    private void ensureTeamPublishAllowed(Long userId, Long teamId) {
        if (teamId != null && !socialClient.isTeamMember(teamId, userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "只有小队成员可以发布队内活动");
        }
    }

    private void incrementTagUsage(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        tags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .forEach(tagMapper::incrementUsage);
    }

    private void ensureCreator(Activity activity, Long creatorId) {
        if (!activity.getCreatorId().equals(creatorId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    private void ensureNotCreator(Activity activity, Long userId) {
        if (activity.getCreatorId().equals(userId)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "发起人不能报名自己的活动");
        }
    }

    private Activity getActivityOrThrow(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "活动不存在");
        }
        return activity;
    }

    private ActivityCreateResponse toCreateResponse(Activity activity) {
        return new ActivityCreateResponse(activity.getId(), activity.getStatus(), statusText(activity.getStatus()));
    }

    private ActivityListItemResponse toListItem(Activity activity) {
        ActivityListItemResponse response = new ActivityListItemResponse();
        response.setActivityId(activity.getId());
        response.setTitle(activity.getTitle());
        response.setCoverUrl(activity.getCoverUrl());
        response.setTags(fromJson(activity.getTags()));
        response.setStartTime(activity.getStartTime());
        response.setLocationName(activity.getLocationName());
        response.setLocationLat(activity.getLocationLat());
        response.setLocationLng(activity.getLocationLng());
        response.setLocationDetail(activity.getLocationDetail());
        response.setCurrentCount(activity.getCurrentCount());
        response.setMaxParticipants(activity.getMaxParticipants());
        response.setFee(activity.getFee());
        response.setStatus(activity.getStatus());
        response.setStatusText(statusText(activity.getStatus()));
        return response;
    }

    private ActivityListItemResponse toNearbyListItem(Activity activity, double lat, double lng) {
        ActivityListItemResponse response = toListItem(activity);
        if (activity.getLocationLat() != null && activity.getLocationLng() != null) {
            double distance = distanceMeters(
                    lat,
                    lng,
                    activity.getLocationLat().doubleValue(),
                    activity.getLocationLng().doubleValue());
            response.setDistanceMeters(Math.round(distance));
        }
        return response;
    }

    private ActivityDetailResponse toDetail(Activity activity) {
        UserBasicDTO creator = userClient.getUsersByIds(List.of(activity.getCreatorId()))
                .stream()
                .filter(user -> activity.getCreatorId().equals(user.getUserId()))
                .findFirst()
                .orElse(null);
        ActivityDetailResponse response = new ActivityDetailResponse();
        response.setActivityId(activity.getId());
        response.setCreatorId(activity.getCreatorId());
        if (creator != null) {
            response.setCreatorNickname(creator.getNickname());
            response.setCreatorAvatarUrl(creator.getAvatarUrl());
            response.setCreatorUserType(creator.getUserType());
        }
        response.setTitle(activity.getTitle());
        response.setDescription(activity.getDescription());
        response.setTags(fromJson(activity.getTags()));
        response.setCoverUrl(activity.getCoverUrl());
        response.setStartTime(activity.getStartTime());
        response.setEndTime(activity.getEndTime());
        response.setRegDeadline(activity.getRegDeadline());
        response.setLocationName(activity.getLocationName());
        response.setLocationLat(activity.getLocationLat());
        response.setLocationLng(activity.getLocationLng());
        response.setLocationDetail(activity.getLocationDetail());
        response.setMaxParticipants(activity.getMaxParticipants());
        response.setCurrentCount(activity.getCurrentCount());
        response.setFee(activity.getFee());
        response.setStatus(activity.getStatus());
        response.setStatusText(statusText(activity.getStatus()));
        response.setReviewType(activity.getReviewType());
        response.setLocationVerify(Integer.valueOf(1).equals(activity.getLocationVerify()));
        response.setLocationRadius(activity.getLocationRadius());
        response.setTemplateId(activity.getTemplateId());
        return response;
    }

    private ActivityTemplateResponse toTemplateResponse(ActivityTemplate template) {
        ActivityTemplateResponse response = new ActivityTemplateResponse();
        response.setTemplateId(template.getId());
        response.setName(template.getName());
        response.setCategory(template.getCategory());
        response.setDescription(template.getDescription());
        response.setDefaultTags(fromJson(template.getDefaultTags()));
        response.setDefaultDuration(template.getDefaultDuration());
        response.setDefaultMaxParticipants(template.getDefaultMaxParticipants());
        response.setSafetyNotes(template.getSafetyNotes());
        response.setCoverUrl(template.getCoverUrl());
        return response;
    }

    private ActivityTagResponse toTagResponse(ActivityTag tag) {
        ActivityTagResponse response = new ActivityTagResponse();
        response.setTagId(tag.getId());
        response.setName(tag.getName());
        response.setCategory(tag.getCategory());
        response.setUsageCount(tag.getUsageCount());
        return response;
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setNotificationId(notification.getId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setContent(notification.getContent());
        response.setRelatedType(notification.getRelatedType());
        response.setRelatedId(notification.getRelatedId());
        response.setRead(Integer.valueOf(1).equals(notification.getReadFlag()));
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Collections.emptyList() : value);
        } catch (JsonProcessingException ex) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "JSON字段格式错误");
        }
    }

    private String toJsonObject(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "JSON字段格式错误");
        }
    }

    private List<String> fromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return Collections.emptyList();
        }
    }

    private String statusText(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "草稿";
            case 1 -> "审核中";
            case 2 -> "已发布";
            case 3 -> "报名中";
            case 4 -> "报名截止";
            case 5 -> "进行中";
            case 6 -> "已结束";
            case 7 -> "已下架";
            case 8 -> "审核驳回";
            default -> "未知";
        };
    }

    private String registrationStatusText(Integer status) {
        return switch (status == null ? -1 : status) {
            case REG_STATUS_REGISTERED -> "registered";
            case REG_STATUS_CANCELLED -> "cancelled";
            default -> "none";
        };
    }

    private String waitlistStatusText(Integer status) {
        return switch (status == null ? -1 : status) {
            case WAIT_STATUS_WAITING -> "waiting";
            case WAIT_STATUS_PENDING -> "pending_confirm";
            case WAIT_STATUS_CANCELLED -> "cancelled";
            case WAIT_STATUS_EXPIRED -> "expired";
            default -> "none";
        };
    }
}
