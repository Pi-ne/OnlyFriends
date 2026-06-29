package com.onlyfriends.user;

import com.onlyfriends.common.storage.FileStorageService;
import com.onlyfriends.user.entity.User;
import com.onlyfriends.user.mapper.UserMapper;
import com.onlyfriends.user.service.UserService;
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
class FileUploadServiceTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Test
    void uploadAvatarStoresFileAndUpdatesUser() {
        User user = new User();
        user.setEmail("upload-avatar@example.com");
        user.setPasswordHash("hash");
        user.setNickname("上传头像用户");
        user.setStatus(1);
        user.setUserType(0);
        user.setCreditScore(100);
        user.setDeleted(0);
        userMapper.insert(user);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        String avatarUrl = userService.uploadAvatar(user.getId(), file);

        assertThat(avatarUrl).startsWith("/uploads/avatar/").endsWith(".png");
        assertThat(userMapper.selectById(user.getId()).getAvatarUrl()).isEqualTo(avatarUrl);
        assertThat(Files.exists(Path.of("target/test-uploads", avatarUrl.replace("/uploads/", "")))).isTrue();
    }

    @Test
    void uploadMerchantLicenseStoresFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "license.pdf",
                "application/pdf",
                new byte[]{4, 5, 6}
        );

        String licenseUrl = fileStorageService.upload("merchant-license", file);

        assertThat(licenseUrl).startsWith("/uploads/merchant-license/").endsWith(".pdf");
        assertThat(Files.exists(Path.of("target/test-uploads", licenseUrl.replace("/uploads/", "")))).isTrue();
    }
}
