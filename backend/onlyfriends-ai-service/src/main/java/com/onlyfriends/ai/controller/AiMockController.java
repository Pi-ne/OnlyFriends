package com.onlyfriends.ai.controller;

import com.onlyfriends.ai.dto.AiActivityPlanRequest;
import com.onlyfriends.ai.dto.AiActivityPlanResponse;
import com.onlyfriends.ai.dto.AiImageClassifyRequest;
import com.onlyfriends.ai.dto.AiImageClassifyResponse;
import com.onlyfriends.ai.dto.AiReviewRequest;
import com.onlyfriends.ai.dto.AiReviewResponse;
import com.onlyfriends.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiMockController {
    private static final List<String> REJECT_KEYWORDS = List.of(
            "fraud", "gambling", "drug", "porn", "terror", "weapon",
            "诈骗", "赌博", "毒品", "色情", "恐怖", "枪支"
    );
    private static final List<String> RISK_KEYWORDS = List.of(
            "alcohol", "night", "danger", "high intensity", "paid", "extreme",
            "酒", "夜间", "危险", "高强度", "收费", "极限"
    );

    @Value("${ai.provider:mock}")
    private String provider;

    @Value("${ai.model:mock-planner-v1}")
    private String model;

    @PostMapping("/plan-activity")
    public Result<AiActivityPlanResponse> planActivity(@RequestBody(required = false) AiActivityPlanRequest request) {
        String theme = text(request == null ? null : request.getTheme(), "城市轻社交");
        String city = text(request == null ? null : request.getLocationName(), "交通便利的公共空间");
        Integer participants = request == null || request.getMaxParticipants() == null ? 12 : request.getMaxParticipants();
        Integer duration = request == null || request.getDurationHours() == null ? 2 : request.getDurationHours();

        AiActivityPlanResponse response = new AiActivityPlanResponse();
        response.setTitle(theme + "体验局");
        response.setDescription("围绕" + theme + "设计的轻量活动，适合破冰交流、共同体验和合影总结。建议选择" + city + "，并提前明确集合点、时间安排和安全边界。");
        response.setTags(List.of(theme, "社交", "体验", participants > 30 ? "多人活动" : "小规模"));
        response.setLocationSuggestion(city);
        response.setSuggestedDurationHours(duration);
        response.setSuggestedMaxParticipants(participants);
        response.setSafetyNotes(List.of("提前确认集合点和联系人", "活动前同步天气与交通信息", "控制强度并预留休息时间"));
        response.setAgenda(List.of("签到与破冰", "主题体验", "自由交流", "总结合影"));
        return Result.success(response);
    }

    @PostMapping("/review-content")
    public Result<AiReviewResponse> reviewContent(@RequestBody AiReviewRequest request) {
        return Result.success(review(request));
    }

    @PostMapping("/classify-images")
    public Result<AiImageClassifyResponse> classifyImages(@RequestBody AiImageClassifyRequest request) {
        List<String> urls = request == null || request.getImageUrls() == null ? List.of() : request.getImageUrls();
        AiImageClassifyResponse response = new AiImageClassifyResponse();
        response.setResults(urls.stream().map(this::classifyImage).toList());
        return Result.success(response);
    }

    private AiReviewResponse review(AiReviewRequest request) {
        String content = (safe(request == null ? null : request.getTitle()) + " "
                + safe(request == null ? null : request.getDescription()) + " "
                + (request == null || request.getTags() == null ? "" : request.getTags()))
                .toLowerCase(Locale.ROOT);
        List<String> rejectHits = hitKeywords(content, REJECT_KEYWORDS);
        List<String> riskHits = hitKeywords(content, RISK_KEYWORDS);

        AiReviewResponse response = new AiReviewResponse();
        if (!rejectHits.isEmpty()) {
            response.setResult("reject");
            response.setRiskLevel(9);
            response.setRiskCategories(toCategories("severe_violation", rejectHits));
            response.setReason("Matched severe violation keywords: " + String.join(",", rejectHits));
            response.setConfidence(new BigDecimal("0.96"));
            return response;
        }
        if (!riskHits.isEmpty()) {
            response.setResult("risk");
            response.setRiskLevel(5);
            response.setRiskCategories(toCategories("safety_risk", riskHits));
            response.setReason("Matched risk keywords: " + String.join(",", riskHits));
            response.setConfidence(new BigDecimal("0.82"));
            return response;
        }
        response.setResult("pass");
        response.setRiskLevel(0);
        response.setRiskCategories(List.of());
        response.setReason("No mock risk keyword matched.");
        response.setConfidence(new BigDecimal("0.93"));
        return response;
    }

    private AiImageClassifyResponse.ImageResult classifyImage(String url) {
        String lower = safe(url).toLowerCase(Locale.ROOT);
        AiImageClassifyResponse.ImageResult result = new AiImageClassifyResponse.ImageResult();
        result.setImageUrl(url);
        if (lower.contains("group") || lower.contains("people") || lower.contains("photo")) {
            result.setCategory("group_photo");
            result.setTags(List.of("合影", "用户"));
        } else if (lower.contains("venue") || lower.contains("site") || lower.contains("place")) {
            result.setCategory("venue");
            result.setTags(List.of("场地", "环境"));
        } else if (lower.contains("process") || lower.contains("record") || lower.contains("run")) {
            result.setCategory("process_record");
            result.setTags(List.of("过程记录", "活动现场"));
        } else if (lower.contains("material") || lower.contains("supply") || lower.contains("kit")) {
            result.setCategory("supplies");
            result.setTags(List.of("物资", "准备清单"));
        } else if (lower.contains("result") || lower.contains("work") || lower.contains("achievement")) {
            result.setCategory("achievement");
            result.setTags(List.of("成果展示", "作品"));
        } else {
            result.setCategory("process_record");
            result.setTags(List.of("活动记录", "待人工确认"));
        }
        result.setModeration(lower.contains("bad") || lower.contains("risk") ? "risk" : "pass");
        result.setConfidence(0.88);
        return result;
    }

    private List<String> hitKeywords(String content, List<String> keywords) {
        return keywords.stream().filter(content::contains).toList();
    }

    private List<String> toCategories(String category, List<String> hits) {
        List<String> categories = new ArrayList<>();
        categories.add(category);
        categories.addAll(hits);
        return categories;
    }

    private String text(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
