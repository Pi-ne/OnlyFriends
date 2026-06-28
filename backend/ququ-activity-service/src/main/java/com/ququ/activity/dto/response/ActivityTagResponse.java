package com.ququ.activity.dto.response;

import lombok.Data;

@Data
public class ActivityTagResponse {
    private Long tagId;
    private String name;
    private String category;
    private Integer usageCount;
}
