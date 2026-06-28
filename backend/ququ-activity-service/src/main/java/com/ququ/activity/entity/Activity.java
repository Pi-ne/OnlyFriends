package com.ququ.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long creatorId;
    private String title;
    private String description;
    private String tags;
    private String coverUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime regDeadline;
    private String locationName;
    private BigDecimal locationLat;
    private BigDecimal locationLng;
    private String locationDetail;
    private Integer maxParticipants;
    private Integer currentCount;
    private BigDecimal fee;
    private Integer status;
    private Integer reviewType;
    private Integer isTeamOnly;
    private Long teamId;
    private Long templateId;
    private Long cloneFromId;
    private String checkinQrCode;
    private Integer locationVerify;
    private Integer locationRadius;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
