package com.onlyfriends.user.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminMerchantApplyResponse {
    private Long applyId;
    private Long userId;
    private String merchantName;
    private String licenseUrl;
    private List<String> focusTags;
    private Integer status;
    private String rejectReason;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
