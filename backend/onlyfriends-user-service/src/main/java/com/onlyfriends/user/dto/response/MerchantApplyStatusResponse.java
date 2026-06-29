package com.onlyfriends.user.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MerchantApplyStatusResponse {
    private Long applyId;
    private String merchantName;
    private String licenseUrl;
    private List<String> focusTags;
    private Integer status;
    private String rejectReason;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
