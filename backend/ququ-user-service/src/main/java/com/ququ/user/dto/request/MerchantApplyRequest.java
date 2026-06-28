package com.ququ.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MerchantApplyRequest {
    @NotBlank(message = "商家名称不能为空")
    @Size(max = 100, message = "商家名称最长100字")
    private String merchantName;

    @NotBlank(message = "营业执照或凭证URL不能为空")
    @Size(max = 500, message = "凭证URL最长500字")
    private String licenseUrl;

    @Size(max = 10, message = "关注领域标签最多10个")
    private List<@Size(max = 20, message = "单个标签最长20字") String> focusTags;
}
