package com.ququ.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_ban_record")
public class UserBanRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long adminId;
    private String reason;
    private LocalDateTime banExpireAt;
    private LocalDateTime createdAt;
}
