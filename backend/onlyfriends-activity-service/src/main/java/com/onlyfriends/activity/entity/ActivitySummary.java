package com.onlyfriends.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_summary")
public class ActivitySummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long creatorId;
    private String title;
    private String content;
    private String imageUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
