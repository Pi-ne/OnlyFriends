package com.onlyfriends.activity.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ActivityQueryRequest {
    private String tab;
    private String keyword;
    private String tags;
    private Integer status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String city;
    private String locationName;
    private Long creatorId;
    private Long teamId;
    private Boolean registered;
    private BigDecimal lat;
    private BigDecimal lng;
    private Integer radius;
    private BigDecimal minFee;
    private BigDecimal maxFee;
    private Integer minParticipants;
    private Integer maxParticipants;
    private Integer page = 1;
    private Integer size = 20;
}
