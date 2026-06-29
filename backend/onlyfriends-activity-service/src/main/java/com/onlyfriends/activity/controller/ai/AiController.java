package com.onlyfriends.activity.controller.ai;

import com.onlyfriends.activity.dto.ai.AiActivityPlanRequest;
import com.onlyfriends.activity.dto.ai.AiActivityPlanResponse;
import com.onlyfriends.activity.dto.ai.AiImageClassifyRequest;
import com.onlyfriends.activity.dto.ai.AiImageClassifyResponse;
import com.onlyfriends.activity.dto.ai.AiReviewRequest;
import com.onlyfriends.activity.dto.ai.AiReviewResponse;
import com.onlyfriends.activity.service.ai.AiClient;
import com.onlyfriends.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiController {
    private final AiClient aiClient;

    @PostMapping("/plan-activity")
    public Result<AiActivityPlanResponse> planActivity(@RequestBody(required = false) AiActivityPlanRequest request) {
        return Result.success(aiClient.planActivity(request));
    }

    @PostMapping("/classify-images")
    public Result<AiImageClassifyResponse> classifyImages(@RequestBody AiImageClassifyRequest request) {
        return Result.success(aiClient.classifyImages(request));
    }

    @PostMapping("/review-content")
    public Result<AiReviewResponse> reviewContent(@RequestBody AiReviewRequest request) {
        return Result.success(aiClient.reviewContent(request));
    }
}
