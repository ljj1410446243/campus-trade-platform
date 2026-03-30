package com.campus.trade.file.service;

import com.campus.trade.file.dto.BindFileRequest;
import com.campus.trade.file.dto.BatchFileUploadItemResponse;
import com.campus.trade.file.dto.FileMetadataResponse;
import com.campus.trade.file.dto.FileUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    FileUploadResponse uploadImage(String ownerId, MultipartFile file, String bizType);

    List<BatchFileUploadItemResponse> uploadImages(String ownerId, MultipartFile[] files, String bizType);

    FileMetadataResponse getFileMetadata(String fileId);

    FileView viewFile(String fileId);

    void bindFiles(String ownerId, BindFileRequest request);

    void deleteFile(String ownerId, String fileId);

    class FileView {
        private final Resource resource;
        private final String contentType;
        private final long size;
        private final String fileName;

        public FileView(Resource resource, String contentType, long size, String fileName) {
            this.resource = resource;
            this.contentType = contentType;
            this.size = size;
            this.fileName = fileName;
        }

        public Resource getResource() {
            return resource;
        }

        public String getContentType() {
            return contentType;
        }

        public long getSize() {
            return size;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
