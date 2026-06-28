package com.ququ.activity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ActivitySummaryRequest {
    @NotBlank(message = "总结标题不能为空")
    @Size(max = 100, message = "总结标题最长100字")
    private String title;

    @NotBlank(message = "总结内容不能为空")
    private String content;

    @Size(max = 20, message = "总结图片最多20张")
    private List<@Size(max = 500, message = "图片URL最长500字") String> imageUrls;
}
