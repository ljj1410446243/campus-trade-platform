package com.campus.trade.file.service;

public class StoredFileInfo {

    private final String storageType;
    private final String originalStoragePath;
    private final String storagePath;
    private final String ext;
    private final String previewStoragePath;
    private final String thumbnailStoragePath;

    public StoredFileInfo(String storageType,
                          String originalStoragePath,
                          String storagePath,
                          String ext,
                          String previewStoragePath,
                          String thumbnailStoragePath) {
        this.storageType = storageType;
        this.originalStoragePath = originalStoragePath;
        this.storagePath = storagePath;
        this.ext = ext;
        this.previewStoragePath = previewStoragePath;
        this.thumbnailStoragePath = thumbnailStoragePath;
    }

    public String getStorageType() {
        return storageType;
    }

    public String getOriginalStoragePath() {
        return originalStoragePath;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getExt() {
        return ext;
    }

    public String getPreviewStoragePath() {
        return previewStoragePath;
    }

    public String getThumbnailStoragePath() {
        return thumbnailStoragePath;
    }
}
