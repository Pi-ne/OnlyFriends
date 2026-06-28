package com.ququ.user.service;

import com.ququ.user.dto.request.MerchantApplyRequest;
import com.ququ.user.dto.response.MerchantInfoResponse;
import com.ququ.user.dto.response.MerchantApplyStatusResponse;

public interface MerchantService {
    Long apply(Long userId, MerchantApplyRequest request);

    MerchantApplyStatusResponse getApplyStatus(Long userId);

    MerchantInfoResponse getMerchantInfo(Long userId);
}
