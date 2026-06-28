package com.ququ.activity.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ququ.activity.dto.ai.AiActivityPlanRequest;
import com.ququ.activity.dto.ai.AiActivityPlanResponse;
import com.ququ.activity.dto.ai.AiImageClassifyRequest;
import com.ququ.activity.dto.ai.AiImageClassifyResponse;
import com.ququ.activity.dto.ai.AiReviewRequest;
import com.ququ.activity.dto.ai.AiReviewResponse;
import com.ququ.common.exception.BizException;
import com.ququ.common.response.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiClientImpl implements AiClient {
    private static final List<String> REJECT_KEYWORDS = List.of(
            "fraud", "gambling", "drug", "porn", "terror", "weapon",
            "诈骗", "赌博", "毒品", "色情", "恐怖", "枪支"
    );
    private static final List<String> RISK_KEYWORDS = List.of(
            "alcohol", "night", "danger", "high intensity", "paid", "extreme",
            "酒", "夜间", "危险", "高强度", "收费", "极限"
    );

    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper;
    private final AiReviewCacheService reviewCacheService;

    @Value("${ai.mode:local}")
    private String mode;

    @Value("${ai.service.url:http://localhost:8001}")
    private String aiServiceUrl;

    @Value("${ai.service.timeout-seconds:30}")
    private long timeoutSeconds;

    @Value("${ai.service.retry-times:1}")
    private int retryTimes;

    @Override
    public AiActivityPlanResponse planActivity(AiActivityPlanRequest request) {
        if (useRemote()) {
            return remote("/ai/plan-activity", request, AiActivityPlanResponse.class);
        }
        String theme = text(request == null ? null : request.getTheme(), "城市轻社交");
        String location = text(request == null ? null : request.getLocationName(), "交通便利的公共空间");
        Integer participants = request == null || request.getMaxParticipants() == null ? 12 : request.getMaxParticipants();
        Integer duration = request == null || request.getDurationHours() == null ? 2 : request.getDurationHours();

        AiActivityPlanResponse response = new AiActivityPlanResponse();
        response.setTitle(theme + "体验局");
        response.setDescription("围绕" + theme + "设计的轻量活动，适合破冰交流、共同体验和合影总结。建议选择" + location + "，并提前明确集合点、时间安排和安全边界。");
        response.setTags(List.of(theme, "社交", "体验", participants > 30 ? "多人活动" : "小规模"));
        response.setLocationSuggestion(location);
        response.setSuggestedDurationHours(duration);
        response.setSuggestedMaxParticipants(participants);
        response.setSafetyNotes(List.of("提前确认集合点和联系人", "活动前同步天气与交通信息", "控制强度并预留休息时间"));
        response.setAgenda(List.of("签到与破冰", "主题体验", "自由交流", "总结合影"));
        return response;
    }

    @Override
    public AiReviewResponse reviewContent(AiReviewRequest request) {
        AiReviewResponse cached = reviewCacheService.get(request);
        if (cached != null) {
            return cached;
        }
        AiReviewResponse response;
        if (useRemote()) {
            response = remoteWithRetry("/ai/review-content", request, AiReviewResponse.class);
        } else {
            response = localReview(request);
        }
        reviewCacheService.put(request, response);
        return response;
    }

    @Override
    public AiImageClassifyResponse classifyImages(AiImageClassifyRequest request) {
        if (useRemote()) {
            return remote("/ai/classify-images", request, AiImageClassifyResponse.class);
        }
        List<String> urls = request == null || request.getImageUrls() == null ? List.of() : request.getImageUrls();
        AiImageClassifyResponse response = new AiImageClassifyResponse();
        response.setResults(urls.stream().map(this::classifyImage).toList());
        return response;
    }

    private boolean useRemote() {
        return "remote".equalsIgnoreCase(mode);
    }

    @SuppressWarnings("unchecked")
    private <T> T remote(String path, Object request, Class<T> responseType) {
        try {
            Map<String, Object> result = remoteRestTemplate().postForObject(aiServiceUrl + path, request, Map.class);
            if (result == null || !Integer.valueOf(200).equals(result.get("code"))) {
                throw new BizException(ResultCode.INTERNAL_ERROR.getCode(), "AI service returned an error");
            }
            return objectMapper.convertValue(result.get("data"), responseType);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ResultCode.INTERNAL_ERROR.getCode(), "AI service call failed");
        }
    }

    private <T> T remoteWithRetry(String path, Object request, Class<T> responseType) {
        int attempts = Math.max(0, retryTimes) + 1;
        RuntimeException lastException = null;
        for (int i = 1; i <= attempts; i++) {
            try {
                return remote(path, request, responseType);
            } catch (RuntimeException ex) {
                lastException = ex;
                log.warn("AI remote call failed, attempt {}/{}", i, attempts, ex);
            }
        }
        throw lastException == null
                ? new BizException(ResultCode.INTERNAL_ERROR.getCode(), "AI service call failed")
                : lastException;
    }

    private RestTemplate remoteRestTemplate() {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    private AiReviewResponse localReview(AiReviewRequest request) {
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
        } else if (!riskHits.isEmpty()) {
            response.setResult("risk");
            response.setRiskLevel(5);
            response.setRiskCategories(toCategories("safety_risk", riskHits));
            response.setReason("Matched risk keywords: " + String.join(",", riskHits));
            response.setConfidence(new BigDecimal("0.82"));
        } else {
            response.setResult("pass");
            response.setRiskLevel(0);
            response.setRiskCategories(List.of());
            response.setReason("No mock risk keyword matched.");
            response.setConfidence(new BigDecimal("0.93"));
        }
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
