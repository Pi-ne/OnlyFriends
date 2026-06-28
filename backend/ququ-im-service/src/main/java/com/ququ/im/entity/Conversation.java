package com.ququ.im.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_conversation")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer convType;
    private Long userIdA;
    private Long userIdB;
    private Long teamId;
    private Long lastMsgId;
    private String lastMsgPreview;
    private LocalDateTime lastMsgAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
