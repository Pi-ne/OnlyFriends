package com.onlyfriends.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("merchant_apply")
public class MerchantApply {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String merchantName;
    private String licenseUrl;
    private String focusTags;
    private Integer status;
    private String rejectReason;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
