package com.ququ.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend_apply")
public class FriendApply {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicantId;
    private Long targetId;
    private String message;
    private Integer status;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
