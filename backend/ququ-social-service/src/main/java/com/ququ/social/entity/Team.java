package com.ququ.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("team")
public class Team {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ownerId;
    private String name;
    private String description;
    private String tags;
    private Integer joinType;
    private Integer maxMembers;
    private Integer memberCount;
    private Integer status;
    private Integer score;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
