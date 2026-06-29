package com.onlyfriends.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_tag")
public class ActivityTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private Integer usageCount;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
