package com.ququ.activity.dto.ai;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AiReviewResponse {
    private String result;
    private Integer riskLevel;
    private List<String> riskCategories;
    private String reason;
    private BigDecimal confidence;
}
