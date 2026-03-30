package com.campus.trade.file.service.impl;

import com.campus.trade.file.config.FileStorageProperties;
import com.campus.trade.file.exception.BusinessException;
import com.campus.trade.file.service.StorageService;
import com.campus.trade.file.service.StoredFileInfo;
import com.campus.trade.file.util.FileConstants;
import com.campus.trade.file.util.FileExtensionUtil;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final Path rootPath;

    public LocalStorageService(FileStorageProperties fileStorageProperties) {
        this.rootPath = Paths.get(fileStorageProperties.getRootDir()).toAbsolutePath().normalize();
    }

    @Override
    public StoredFileInfo store(MultipartFile file, String bizType) {
        String ext = FileExtensionUtil.resolveExtension(file);
        String datePath = DATE_FORMATTER.format(LocalDate.now());
        String storedFileName = UUID.randomUUID() + "." + ext;
        String relativePath = bizType.toLowerCase() + "/" + datePath + "/" + storedFileName;
        Path targetPath = rootPath.resolve(relativePath).normalize();

        try {
            Files.createDirectories(targetPath.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException(500, "文件存储失败");
        }

        return new StoredFileInfo(FileConstants.STORAGE_LOCAL, relativePath.replace("\\", "/"), ext);
    }

    @Override
    public Resource loadAsResource(String storagePath) {
        try {
            Path filePath = rootPath.resolve(storagePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(404, "文件不存在");
            }
            return resource;
        } catch (IOException e) {
            throw new BusinessException(404, "文件不存在");
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(rootPath.resolve(storagePath).normalize());
        } catch (IOException e) {
            throw new BusinessException(500, "文件删除失败");
        }
    }
}
