package com.ququ.activity.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class AiImageClassifyResponse {
    private List<ImageResult> results;

    @Data
    public static class ImageResult {
        private String imageUrl;
        private String category;
        private List<String> tags;
        private String moderation;
        private Double confidence;
    }
}
