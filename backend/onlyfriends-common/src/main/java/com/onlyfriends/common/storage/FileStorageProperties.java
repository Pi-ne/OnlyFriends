package com.onlyfriends.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "app.storage")
public class FileStorageProperties {
    private String type = "local";
    private String localDir = "data/uploads";
    private String publicBaseUrl = "";
    private Minio minio = new Minio();
    private Map<String, String> buckets = defaultBuckets();

    public String bucket(String purpose) {
        return buckets.getOrDefault(purpose, buckets.getOrDefault("default", "onlyfriends-files"));
    }

    private static Map<String, String> defaultBuckets() {
        Map<String, String> values = new HashMap<>();
        values.put("default", "onlyfriends-files");
        values.put("avatar", "onlyfriends-user-avatars");
        values.put("merchant-license", "onlyfriends-merchant-licenses");
        values.put("activity-image", "onlyfriends-activity-images");
        values.put("team-file", "onlyfriends-team-files");
        values.put("team-album", "onlyfriends-team-album");
        return values;
    }

    @Data
    public static class Minio {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin123";
    }
}
