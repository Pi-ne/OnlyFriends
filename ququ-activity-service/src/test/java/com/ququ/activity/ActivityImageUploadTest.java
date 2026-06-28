package com.ququ.activity;

import com.ququ.common.storage.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class ActivityImageUploadTest {
    @Autowired
    private FileStorageService fileStorageService;

    @Test
    void uploadActivityImageStoresFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.webp",
                "image/webp",
                new byte[]{7, 8, 9}
        );

        String imageUrl = fileStorageService.upload("activity-image", file);

        assertThat(imageUrl).startsWith("/uploads/activity-image/").endsWith(".webp");
        assertThat(Files.exists(Path.of("target/test-uploads", imageUrl.replace("/uploads/", "")))).isTrue();
    }
}
