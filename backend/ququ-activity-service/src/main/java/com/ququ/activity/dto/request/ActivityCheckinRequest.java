package com.ququ.activity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ActivityCheckinRequest {
    @NotBlank(message = "签到二维码内容不能为空")
    private String qrcodeContent;
    private BigDecimal lat;
    private BigDecimal lng;
}
