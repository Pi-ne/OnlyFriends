package com.onlyfriends.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("merchant_info")
public class MerchantInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String merchantName;
    private String merchantNick;
    private String focusTags;
    private String licenseUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
