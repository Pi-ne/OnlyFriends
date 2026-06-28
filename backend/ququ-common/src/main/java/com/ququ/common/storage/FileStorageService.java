package com.ququ.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String upload(String purpose, MultipartFile file);
}
