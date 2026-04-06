package com.campus.trade.file.controller;

import com.campus.trade.common.response.ApiResponse;
import com.campus.trade.file.dto.BindFileRequest;
import com.campus.trade.file.dto.BatchFileUploadItemResponse;
import com.campus.trade.file.dto.FileMetadataResponse;
import com.campus.trade.file.dto.FileUploadResponse;
import com.campus.trade.file.exception.BusinessException;
import com.campus.trade.file.service.FileService;
import com.campus.trade.file.util.LoginUserHelper;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;
    private final LoginUserHelper loginUserHelper;

    public FileController(FileService fileService, LoginUserHelper loginUserHelper) {
        this.fileService = fileService;
        this.loginUserHelper = loginUserHelper;
    }

    @PostMapping({"/upload/image", "/upload/image/"})
    public ApiResponse<FileUploadResponse> uploadImage(
            @RequestHeader("Authorization") String authorizationHeader,
            MultipartHttpServletRequest request,
            @RequestParam("bizType") String bizType) {

        String ownerId = loginUserHelper.getCurrentUserId(authorizationHeader);
        MultipartFile file = resolveSingleUploadFile(request);
        return ApiResponse.success(fileService.uploadImage(ownerId, file, bizType));
    }

    @PostMapping({"/upload/images", "/upload/images/"})
    public ApiResponse<List<BatchFileUploadItemResponse>> uploadImages(
            @RequestHeader("Authorization") String authorizationHeader,
            MultipartHttpServletRequest request,
            @RequestParam("bizType") String bizType) {

        String ownerId = loginUserHelper.getCurrentUserId(authorizationHeader);
        MultipartFile[] files = resolveBatchUploadFiles(request);
        return ApiResponse.success(fileService.uploadImages(ownerId, files, bizType));
    }

    @GetMapping("/{fileId}")
    public ApiResponse<FileMetadataResponse> getFileMetadata(@PathVariable String fileId) {
        return ApiResponse.success(fileService.getFileMetadata(fileId));
    }

    @GetMapping("/view/{fileId}")
    public ResponseEntity<Resource> viewFile(@PathVariable String fileId) {
        FileService.FileView fileView = fileService.viewFile(fileId);
        String contentType = fileView.getContentType() == null || fileView.getContentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : fileView.getContentType();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(fileView.getSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileView.getFileName() + "\"")
                .body(fileView.getResource());
    }

    @PostMapping("/bind")
    public ApiResponse<Void> bindFiles(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody BindFileRequest request) {

        String ownerId = loginUserHelper.getCurrentUserId(authorizationHeader);
        fileService.bindFiles(ownerId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Map<String, String>> deleteFile(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String fileId) {

        String ownerId = loginUserHelper.getCurrentUserId(authorizationHeader);
        fileService.deleteFile(ownerId, fileId);
        return ApiResponse.success(Map.of("fileId", fileId, "status", "DELETED"));
    }

    private MultipartFile resolveSingleUploadFile(MultipartHttpServletRequest request) {
        MultipartFile file = findFirstFile(request, "file", "file[]", "files", "files[]");
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        return file;
    }

    private MultipartFile[] resolveBatchUploadFiles(MultipartHttpServletRequest request) {
        List<MultipartFile> files = findFiles(request, "files", "files[]", "file", "file[]");
        if (files.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        return files.toArray(new MultipartFile[0]);
    }

    private MultipartFile findFirstFile(MultipartHttpServletRequest request, String... fieldNames) {
        for (String fieldName : fieldNames) {
            List<MultipartFile> files = request.getMultiFileMap().get(fieldName);
            if (files == null) {
                continue;
            }
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    return file;
                }
            }
        }

        for (List<MultipartFile> files : request.getMultiFileMap().values()) {
            if (files == null) {
                continue;
            }
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    return file;
                }
            }
        }

        return null;
    }

    private List<MultipartFile> findFiles(MultipartHttpServletRequest request, String... fieldNames) {
        for (String fieldName : fieldNames) {
            List<MultipartFile> files = request.getMultiFileMap().get(fieldName);
            if (files == null || files.isEmpty()) {
                continue;
            }

            List<MultipartFile> validFiles = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    validFiles.add(file);
                }
            }
            if (!validFiles.isEmpty()) {
                return validFiles;
            }
        }

        List<MultipartFile> fallbackFiles = new ArrayList<>();
        for (List<MultipartFile> files : request.getMultiFileMap().values()) {
            if (files == null) {
                continue;
            }
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    fallbackFiles.add(file);
                }
            }
        }
        return fallbackFiles;
    }
}
