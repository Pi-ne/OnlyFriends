package com.ququ.social.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TeamFileRequest {
    @NotBlank(message = "文件名不能为空")
    @Size(max = 200, message = "文件名不能超过200字")
    private String fileName;

    @NotBlank(message = "文件地址不能为空")
    @Size(max = 500, message = "文件地址不能超过500字")
    private String fileUrl;

    private Long fileSize;
}
