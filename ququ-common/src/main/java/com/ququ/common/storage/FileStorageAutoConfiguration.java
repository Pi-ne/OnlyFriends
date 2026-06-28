package com.ququ.common.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ququ.common.exception.BizException;
import com.ququ.common.response.ResultCode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@AutoConfiguration
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageAutoConfiguration {
    @Bean
    public FileStorageService fileStorageService(FileStorageProperties properties) {
        if ("minio".equalsIgnoreCase(properties.getType())) {
            MinioClient client = MinioClient.builder()
                    .endpoint(properties.getMinio().getEndpoint())
                    .credentials(properties.getMinio().getAccessKey(), properties.getMinio().getSecretKey())
                    .build();
            return new MinioFileStorageService(properties, client);
        }
        return new LocalFileStorageService(properties);
    }

    @RequiredArgsConstructor
    private abstract static class AbstractFileStorageService implements FileStorageService {
        private static final long MAX_SIZE = 10L * 1024 * 1024;
        private static final Set<String> ALLOWED_TYPES = Set.of(
                "image/jpeg",
                "image/png",
                "image/webp",
                "application/pdf"
        );

        protected final FileStorageProperties properties;

        @Override
        public String upload(String purpose, MultipartFile file) {
            validate(file);
            String objectName = buildObjectName(purpose, file.getOriginalFilename());
            try {
                return doUpload(purpose, objectName, file);
            } catch (IOException ex) {
                throw new BizException(ResultCode.INTERNAL_ERROR.getCode(), "文件上传失败");
            }
        }

        protected abstract String doUpload(String purpose, String objectName, MultipartFile file) throws IOException;

        protected String publicUrl(String objectPath) {
            String baseUrl = properties.getPublicBaseUrl();
            if (!StringUtils.hasText(baseUrl)) {
                return "/" + objectPath;
            }
            return baseUrl.replaceAll("/+$", "") + "/" + objectPath;
        }

        private void validate(MultipartFile file) {
            if (file == null || file.isEmpty()) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(), "上传文件不能为空");
            }
            if (file.getSize() > MAX_SIZE) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(), "上传文件不能超过10MB");
            }
            String contentType = file.getContentType();
            if (!StringUtils.hasText(contentType) || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(), "仅支持jpg、png、webp图片或pdf文件");
            }
            String originalFilename = file.getOriginalFilename();
            if (StringUtils.hasText(originalFilename) && originalFilename.contains("..")) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(), "文件名不合法");
            }
        }

        private String buildObjectName(String purpose, String originalFilename) {
            String extension = extension(originalFilename);
            return sanitize(purpose) + "/" + UUID.randomUUID() + extension;
        }

        private String extension(String originalFilename) {
            if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
                return "";
            }
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            return ext.length() > 20 ? "" : ext;
        }

        private String sanitize(String value) {
            if (!StringUtils.hasText(value)) {
                return "default";
            }
            return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        }
    }

    private static class LocalFileStorageService extends AbstractFileStorageService {
        LocalFileStorageService(FileStorageProperties properties) {
            super(properties);
        }

        @Override
        protected String doUpload(String purpose, String objectName, MultipartFile file) throws IOException {
            Path root = Path.of(properties.getLocalDir()).toAbsolutePath().normalize();
            Path target = root.resolve(objectName).normalize();
            if (!target.startsWith(root)) {
                throw new BizException(ResultCode.BAD_REQUEST.getCode(), "文件路径不合法");
            }
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return publicUrl("uploads/" + objectName.replace('\\', '/'));
        }
    }

    private static class MinioFileStorageService extends AbstractFileStorageService {
        private final MinioClient client;

        MinioFileStorageService(FileStorageProperties properties, MinioClient client) {
            super(properties);
            this.client = client;
        }

        @Override
        protected String doUpload(String purpose, String objectName, MultipartFile file) throws IOException {
            String bucket = properties.bucket(purpose);
            try {
                ensureBucket(bucket);
                try (InputStream inputStream = file.getInputStream()) {
                    client.putObject(PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
                }
                return publicUrl(bucket + "/" + objectName);
            } catch (BizException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BizException(ResultCode.INTERNAL_ERROR.getCode(), "MinIO文件上传失败");
            }
        }

        private void ensureBucket(String bucket) throws Exception {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        }
    }
}
