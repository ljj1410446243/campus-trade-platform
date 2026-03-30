package com.campus.trade.file.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    StoredFileInfo store(MultipartFile file, String bizType);

    Resource loadAsResource(String storagePath);

    void delete(String storagePath);
}
