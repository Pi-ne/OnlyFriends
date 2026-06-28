package com.ququ.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ququ.activity.dto.ai.AiReviewRequest;
import com.ququ.activity.dto.ai.AiReviewResponse;
import com.ququ.activity.service.ai.AiClientImpl;
import com.ququ.activity.service.ai.AiReviewCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.web.client.MockRestServiceServer;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiClientImplTest {
    @Test
    void reviewContentUsesCacheBeforeRemoteCall() {
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        AiReviewCacheService cacheService = mock(AiReviewCacheService.class);
        AiClientImpl client = client(builder, cacheService);
        AiReviewRequest request = request();
        AiReviewResponse cached = response("pass");
        when(cacheService.get(request)).thenReturn(cached);

        AiReviewResponse result = client.reviewContent(request);

        assertThat(result).isSameAs(cached);
        verify(builder, never()).build();
    }

    @Test
    void reviewContentRetriesRemoteFailureAndCachesSuccess() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.setConnectTimeout(org.mockito.ArgumentMatchers.any())).thenReturn(builder);
        when(builder.setReadTimeout(org.mockito.ArgumentMatchers.any())).thenReturn(builder);
        when(builder.build()).thenReturn(restTemplate);
        AiReviewCacheService cacheService = mock(AiReviewCacheService.class);
        AiClientImpl client = client(builder, cacheService);
        AiReviewRequest request = request();

        server.expect(once(), requestTo("http://ai.test/ai/review-content"))
                .andRespond(withServerError());
        server.expect(once(), requestTo("http://ai.test/ai/review-content"))
                .andRespond(withSuccess("""
                        {"code":200,"message":"success","data":{"result":"pass","riskLevel":0,"riskCategories":[],"reason":"ok","confidence":0.93},"timestamp":1}
                        """, APPLICATION_JSON));

        AiReviewResponse result = client.reviewContent(request);

        assertThat(result.getResult()).isEqualTo("pass");
        verify(cacheService).put(request, result);
        server.verify();
    }

    private AiClientImpl client(RestTemplateBuilder builder, AiReviewCacheService cacheService) {
        AiClientImpl client = new AiClientImpl(builder, new ObjectMapper(), cacheService);
        ReflectionTestUtils.setField(client, "mode", "remote");
        ReflectionTestUtils.setField(client, "aiServiceUrl", "http://ai.test");
        ReflectionTestUtils.setField(client, "timeoutSeconds", 30L);
        ReflectionTestUtils.setField(client, "retryTimes", 1);
        return client;
    }

    private AiReviewRequest request() {
        AiReviewRequest request = new AiReviewRequest();
        request.setTitle("周末徒步");
        request.setDescription("轻量城市活动");
        request.setTags(List.of("徒步", "社交"));
        request.setMaxParticipants(20);
        return request;
    }

    private AiReviewResponse response(String result) {
        AiReviewResponse response = new AiReviewResponse();
        response.setResult(result);
        response.setRiskLevel(0);
        response.setRiskCategories(List.of());
        response.setReason("cached");
        response.setConfidence(new BigDecimal("0.93"));
        return response;
    }
}
