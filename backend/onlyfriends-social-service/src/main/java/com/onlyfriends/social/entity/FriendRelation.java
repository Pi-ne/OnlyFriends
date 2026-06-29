package com.onlyfriends.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend_relation")
public class FriendRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userIdA;
    private Long userIdB;
    private Integer status;
    private String remarkA;
    private String remarkB;
    private String groupA;
    private String groupB;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
