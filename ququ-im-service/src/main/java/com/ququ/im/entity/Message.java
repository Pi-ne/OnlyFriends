package com.ququ.im.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long convId;
    private Integer convType;
    private Long senderId;
    private Long receiverId;
    private Long teamId;
    private Integer msgType;
    private String content;
    private Boolean mentionAll;
    private String mentionUserIds;
    private String relatedType;
    private Long relatedId;
    private Integer status;
    private LocalDateTime recalledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
