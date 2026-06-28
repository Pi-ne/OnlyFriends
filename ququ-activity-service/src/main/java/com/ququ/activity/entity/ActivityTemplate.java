package com.ququ.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_template")
public class ActivityTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private String description;
    private String defaultTags;
    private Integer defaultDuration;
    private Integer defaultMaxParticipants;
    private String safetyNotes;
    private String coverUrl;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
