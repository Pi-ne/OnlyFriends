package com.onlyfriends.activity.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityCreateRequest {
    @NotBlank(message = "活动名称不能为空")
    @Size(min = 2, max = 100, message = "活动名称长度需在2-100字之间")
    private String title;
    private String description;
    @Size(max = 5, message = "活动标签最多5个")
    private List<@Size(max = 20, message = "单个标签最长20字") String> tags;
    @Size(max = 500, message = "封面图URL最长500字")
    private String coverUrl;
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
    @NotNull(message = "报名截止时间不能为空")
    private LocalDateTime regDeadline;
    @NotBlank(message = "地点名称不能为空")
    @Size(max = 200, message = "地点名称最长200字")
    private String locationName;
    @NotNull(message = "纬度不能为空")
    private BigDecimal locationLat;
    @NotNull(message = "经度不能为空")
    private BigDecimal locationLng;
    @Size(max = 500, message = "详细地址最长500字")
    private String locationDetail;
    @NotNull(message = "人数上限不能为空")
    @Min(value = 0, message = "人数上限不能小于0")
    private Integer maxParticipants;
    @NotNull(message = "费用不能为空")
    @DecimalMin(value = "0.00", message = "费用不能小于0")
    private BigDecimal fee;
    private Integer locationVerify;
    private Integer locationRadius;
    private Long templateId;
    private Long teamId;
    @NotNull(message = "是否草稿不能为空")
    private Boolean isDraft;
}
