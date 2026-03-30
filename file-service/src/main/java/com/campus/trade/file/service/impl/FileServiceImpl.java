package com.campus.trade.file.service.impl;

import com.campus.trade.file.config.FileStorageProperties;
import com.campus.trade.file.dto.BindFileRequest;
import com.campus.trade.file.dto.BatchFileUploadItemResponse;
import com.campus.trade.file.dto.FileMetadataResponse;
import com.campus.trade.file.dto.FileUploadResponse;
import com.campus.trade.file.exception.BusinessException;
import com.campus.trade.file.model.FileDocument;
import com.campus.trade.file.repository.FileRepository;
import com.campus.trade.file.service.FileService;
import com.campus.trade.file.service.StorageService;
import com.campus.trade.file.service.StoredFileInfo;
import com.campus.trade.file.util.FileBizType;
import com.campus.trade.file.util.FileConstants;
import com.campus.trade.file.util.ImageMetadataUtil;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final FileStorageProperties fileStorageProperties;

    public FileServiceImpl(FileRepository fileRepository,
                           StorageService storageService,
                           FileStorageProperties fileStorageProperties) {
        this.fileRepository = fileRepository;
        this.storageService = storageService;
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public FileUploadResponse uploadImage(String ownerId, MultipartFile file, String bizType) {
        String normalizedBizType = FileBizType.normalize(bizType);
        validateFiles(normalizedBizType, new MultipartFile[]{file});
        return toUploadResponse(saveFile(ownerId, file, normalizedBizType));
    }

    @Override
    public List<BatchFileUploadItemResponse> uploadImages(String ownerId, MultipartFile[] files, String bizType) {
        String normalizedBizType = FileBizType.normalize(bizType);
        validateFiles(normalizedBizType, files);

        List<BatchFileUploadItemResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            FileDocument saved = saveFile(ownerId, file, normalizedBizType);
            responses.add(new BatchFileUploadItemResponse(saved.getId(), saved.getUrl()));
        }
        return responses;
    }

    @Override
    public FileMetadataResponse getFileMetadata(String fileId) {
        return toMetadataResponse(getFileOrThrow(fileId));
    }

    @Override
    public FileView viewFile(String fileId) {
        FileDocument fileDocument = getFileOrThrow(fileId);
        if (FileConstants.STATUS_DELETED.equals(fileDocument.getStatus())) {
            throw new BusinessException(404, "文件不存在");
        }

        Resource resource = storageService.loadAsResource(fileDocument.getStoragePath());
        return new FileView(
                resource,
                fileDocument.getContentType(),
                fileDocument.getSize() == null ? 0L : fileDocument.getSize(),
                buildViewFileName(fileDocument)
        );
    }

    @Override
    public void bindFiles(String ownerId, BindFileRequest request) {
        List<FileDocument> fileDocuments = fileRepository.findAllById(request.getFileIds());
        if (fileDocuments.size() != request.getFileIds().size()) {
            throw new BusinessException(404, "存在文件不存在");
        }

        Date now = new Date();
        for (FileDocument fileDocument : fileDocuments) {
            validateOwner(ownerId, fileDocument);
            if (FileConstants.STATUS_DELETED.equals(fileDocument.getStatus())) {
                throw new BusinessException("已删除文件不可绑定");
            }

            fileDocument.setStatus(FileConstants.STATUS_BOUND);
            fileDocument.setBindService(request.getBindService());
            fileDocument.setBindBizId(request.getBindBizId());
            fileDocument.setUpdatedAt(now);
        }

        fileRepository.saveAll(fileDocuments);
    }

    @Override
    public void deleteFile(String ownerId, String fileId) {
        FileDocument fileDocument = getFileOrThrow(fileId);
        validateOwner(ownerId, fileDocument);

        if (FileConstants.STATUS_DELETED.equals(fileDocument.getStatus())) {
            return;
        }

        fileDocument.setStatus(FileConstants.STATUS_DELETED);
        fileDocument.setUpdatedAt(new Date());
        fileRepository.save(fileDocument);
    }

    private FileDocument saveFile(String ownerId, MultipartFile file, String bizType) {
        ImageMetadataUtil.ImageMetadata imageMetadata = ImageMetadataUtil.read(file);
        StoredFileInfo storedFileInfo = storageService.store(file, bizType);

        Date now = new Date();
        FileDocument fileDocument = new FileDocument();
        fileDocument.setOwnerId(ownerId);
        fileDocument.setBizType(bizType);
        fileDocument.setOriginalName(file.getOriginalFilename());
        fileDocument.setExt(storedFileInfo.getExt());
        fileDocument.setContentType(file.getContentType());
        fileDocument.setSize(file.getSize());
        fileDocument.setStorageType(storedFileInfo.getStorageType());
        fileDocument.setStoragePath(storedFileInfo.getStoragePath());
        fileDocument.setWidth(imageMetadata.width());
        fileDocument.setHeight(imageMetadata.height());
        fileDocument.setStatus(FileConstants.STATUS_UPLOADED);
        fileDocument.setCreatedAt(now);
        fileDocument.setUpdatedAt(now);

        FileDocument saved = fileRepository.save(fileDocument);
        saved.setUrl(buildFileUrl(saved.getId()));
        saved.setUpdatedAt(new Date());
        return fileRepository.save(saved);
    }

    private void validateFiles(String bizType, MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BusinessException("上传文件不能为空");
        }

        int maxCount = FileBizType.maxFileCount(bizType);
        if (files.length > maxCount) {
            if (FileBizType.AVATAR.equals(bizType)) {
                throw new BusinessException("头像一次只能上传1张");
            }
            throw new BusinessException("商品图片一次最多上传9张");
        }

        long maxSize = FileBizType.maxFileSize(bizType);
        for (MultipartFile file : files) {
            validateSingleFile(file, bizType, maxSize);
        }
    }

    private void validateSingleFile(MultipartFile file, String bizType, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String contentType = file.getContentType();
        if (!FileConstants.ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException("仅支持 JPEG、PNG、WEBP 图片");
        }

        if (file.getSize() > maxSize) {
            if (FileBizType.AVATAR.equals(bizType)) {
                throw new BusinessException("头像图片大小不能超过2MB");
            }
            throw new BusinessException("商品图片大小不能超过5MB");
        }
    }

    private FileDocument getFileOrThrow(String fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(404, "文件不存在"));
    }

    private void validateOwner(String ownerId, FileDocument fileDocument) {
        if (!ownerId.equals(fileDocument.getOwnerId())) {
            throw new BusinessException(403, "无权限操作该文件");
        }
    }

    private String buildFileUrl(String fileId) {
        return fileStorageProperties.getPublicBaseUrl() + "/files/view/" + fileId;
    }

    private String buildViewFileName(FileDocument fileDocument) {
        String ext = fileDocument.getExt();
        if (ext == null || ext.isBlank()) {
            return fileDocument.getId();
        }
        return fileDocument.getId() + "." + ext;
    }

    private FileUploadResponse toUploadResponse(FileDocument fileDocument) {
        FileUploadResponse response = new FileUploadResponse();
        response.setFileId(fileDocument.getId());
        response.setBizType(fileDocument.getBizType());
        response.setUrl(fileDocument.getUrl());
        response.setContentType(fileDocument.getContentType());
        response.setSize(fileDocument.getSize());
        response.setWidth(fileDocument.getWidth());
        response.setHeight(fileDocument.getHeight());
        response.setStatus(fileDocument.getStatus());
        return response;
    }

    private FileMetadataResponse toMetadataResponse(FileDocument fileDocument) {
        FileMetadataResponse response = new FileMetadataResponse();
        response.setFileId(fileDocument.getId());
        response.setOwnerId(fileDocument.getOwnerId());
        response.setBizType(fileDocument.getBizType());
        response.setOriginalName(fileDocument.getOriginalName());
        response.setExt(fileDocument.getExt());
        response.setContentType(fileDocument.getContentType());
        response.setSize(fileDocument.getSize());
        response.setStorageType(fileDocument.getStorageType());
        response.setStoragePath(fileDocument.getStoragePath());
        response.setUrl(fileDocument.getUrl());
        response.setWidth(fileDocument.getWidth());
        response.setHeight(fileDocument.getHeight());
        response.setStatus(fileDocument.getStatus());
        response.setBindService(fileDocument.getBindService());
        response.setBindBizId(fileDocument.getBindBizId());
        response.setCreatedAt(fileDocument.getCreatedAt());
        response.setUpdatedAt(fileDocument.getUpdatedAt());
        return response;
    }
}
