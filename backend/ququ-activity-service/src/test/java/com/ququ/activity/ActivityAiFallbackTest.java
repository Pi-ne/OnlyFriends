package com.ququ.activity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ququ.activity.dto.ai.AiReviewRequest;
import com.ququ.activity.dto.request.ActivityCreateRequest;
import com.ququ.activity.dto.response.ActivityCreateResponse;
import com.ququ.activity.entity.ActivityReviewRecord;
import com.ququ.activity.mapper.ActivityReviewRecordMapper;
import com.ququ.activity.service.ActivityService;
import com.ququ.activity.service.UserClient;
import com.ququ.activity.service.ai.AiClient;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
class ActivityAiFallbackTest {
    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivityReviewRecordMapper reviewRecordMapper;

    @MockBean
    private UserClient userClient;

    @MockBean
    private AiClient aiClient;

    @Test
    void submitTransfersToManualReviewWhenAiUnavailable() {
        when(userClient.isUserValid(anyLong())).thenReturn(true);
        when(aiClient.reviewContent(any(AiReviewRequest.class)))
                .thenThrow(new BizException(500, "AI service call failed"));

        ActivityCreateResponse response = activityService.create(10001L, request());

        assertThat(response.getStatus()).isEqualTo(1);
        ActivityReviewRecord record = reviewRecordMapper.selectOne(new LambdaQueryWrapper<ActivityReviewRecord>()
                .eq(ActivityReviewRecord::getActivityId, response.getActivityId())
                .orderByDesc(ActivityReviewRecord::getId)
                .last("LIMIT 1"));
        assertThat(record.getAiResult()).isEqualTo("error");
        assertThat(record.getAiRiskCategories()).contains("ai_unavailable");
        assertThat(record.getFinalResult()).isEqualTo(3);
        assertThat(record.getReviewComment()).contains("AI unavailable");
    }

    private ActivityCreateRequest request() {
        ActivityCreateRequest request = new ActivityCreateRequest();
        request.setTitle("AI 降级测试活动");
        request.setDescription("测试 AI 不可用时转人工审核");
        request.setTags(List.of("测试", "活动"));
        request.setCoverUrl("https://example.com/cover.jpg");
        request.setStartTime(LocalDateTime.now().plusDays(3));
        request.setEndTime(LocalDateTime.now().plusDays(3).plusHours(2));
        request.setRegDeadline(LocalDateTime.now().plusDays(2));
        request.setLocationName("测试地点");
        request.setLocationLat(new BigDecimal("39.9289000"));
        request.setLocationLng(new BigDecimal("116.4833000"));
        request.setMaxParticipants(20);
        request.setFee(BigDecimal.ZERO);
        request.setIsDraft(false);
        return request;
    }
}
