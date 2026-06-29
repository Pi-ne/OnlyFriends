package com.onlyfriends.activity.service.ai;

import com.onlyfriends.activity.dto.ai.AiActivityPlanRequest;
import com.onlyfriends.activity.dto.ai.AiActivityPlanResponse;
import com.onlyfriends.activity.dto.ai.AiImageClassifyRequest;
import com.onlyfriends.activity.dto.ai.AiImageClassifyResponse;
import com.onlyfriends.activity.dto.ai.AiReviewRequest;
import com.onlyfriends.activity.dto.ai.AiReviewResponse;

public interface AiClient {
    AiActivityPlanResponse planActivity(AiActivityPlanRequest request);

    AiReviewResponse reviewContent(AiReviewRequest request);

    AiImageClassifyResponse classifyImages(AiImageClassifyRequest request);
}
