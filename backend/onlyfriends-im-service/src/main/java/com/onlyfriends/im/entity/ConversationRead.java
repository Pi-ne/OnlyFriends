package com.onlyfriends.im.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_conversation_read")
public class ConversationRead {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long convId;
    private Long userId;
    private Long lastReadMsgId;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
