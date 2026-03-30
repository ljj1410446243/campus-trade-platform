package com.campus.trade.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class BindFileRequest {

    @NotEmpty(message = "fileIds不能为空")
    private List<String> fileIds;

    @NotBlank(message = "bindService不能为空")
    private String bindService;

    @NotBlank(message = "bindBizId不能为空")
    private String bindBizId;

    public List<String> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<String> fileIds) {
        this.fileIds = fileIds;
    }

    public String getBindService() {
        return bindService;
    }

    public void setBindService(String bindService) {
        this.bindService = bindService;
    }

    public String getBindBizId() {
        return bindBizId;
    }

    public void setBindBizId(String bindBizId) {
        this.bindBizId = bindBizId;
    }
}
