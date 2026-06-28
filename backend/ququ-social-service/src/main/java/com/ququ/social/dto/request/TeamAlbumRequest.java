package com.ququ.social.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TeamAlbumRequest {
    @NotBlank(message = "图片地址不能为空")
    @Size(max = 500, message = "图片地址不能超过500字")
    private String imageUrl;

    @Size(max = 200, message = "图片描述不能超过200字")
    private String description;
}
