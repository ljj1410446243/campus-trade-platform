package com.campus.trade.file.service;

public class StoredFileInfo {

    private final String storageType;
    private final String storagePath;
    private final String ext;

    public StoredFileInfo(String storageType, String storagePath, String ext) {
        this.storageType = storageType;
        this.storagePath = storagePath;
        this.ext = ext;
    }

    public String getStorageType() {
        return storageType;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getExt() {
        return ext;
    }
}
