package com.onlyfriends.user.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class MerchantInfoResponse {
    private Long userId;
    private String merchantName;
    private String merchantNick;
    private List<String> focusTags;
    private String licenseUrl;
}
