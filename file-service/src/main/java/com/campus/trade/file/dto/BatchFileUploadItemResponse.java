package com.campus.trade.file.dto;

public class BatchFileUploadItemResponse {

    private String fileId;
    private String url;

    public BatchFileUploadItemResponse() {
    }

    public BatchFileUploadItemResponse(String fileId, String url) {
        this.fileId = fileId;
        this.url = url;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
