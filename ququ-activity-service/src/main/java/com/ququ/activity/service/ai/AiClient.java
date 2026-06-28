package com.ququ.activity.service.ai;

import com.ququ.activity.dto.ai.AiActivityPlanRequest;
import com.ququ.activity.dto.ai.AiActivityPlanResponse;
import com.ququ.activity.dto.ai.AiImageClassifyRequest;
import com.ququ.activity.dto.ai.AiImageClassifyResponse;
import com.ququ.activity.dto.ai.AiReviewRequest;
import com.ququ.activity.dto.ai.AiReviewResponse;

public interface AiClient {
    AiActivityPlanResponse planActivity(AiActivityPlanRequest request);

    AiReviewResponse reviewContent(AiReviewRequest request);

    AiImageClassifyResponse classifyImages(AiImageClassifyRequest request);
}
