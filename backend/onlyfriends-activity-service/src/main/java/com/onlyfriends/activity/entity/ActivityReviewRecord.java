package com.onlyfriends.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("activity_review_record")
public class ActivityReviewRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Integer reviewStage;
    private Long reviewerId;
    private String aiResult;
    private Integer aiRiskLevel;
    private String aiRiskCategories;
    private String aiReason;
    private BigDecimal aiConfidence;
    private Integer finalResult;
    private String reviewComment;
    private LocalDateTime createdAt;
}
