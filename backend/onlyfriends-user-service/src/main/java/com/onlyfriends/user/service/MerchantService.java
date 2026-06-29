package com.onlyfriends.user.service;

import com.onlyfriends.user.dto.request.MerchantApplyRequest;
import com.onlyfriends.user.dto.response.MerchantInfoResponse;
import com.onlyfriends.user.dto.response.MerchantApplyStatusResponse;

public interface MerchantService {
    Long apply(Long userId, MerchantApplyRequest request);

    MerchantApplyStatusResponse getApplyStatus(Long userId);

    MerchantInfoResponse getMerchantInfo(Long userId);
}
