package com.campus.trade.user.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateReportRequest {

    @NotBlank(message = "reportType不能为空")
    private String reportType;

    @NotBlank(message = "targetId不能为空")
    private String targetId;

    @NotBlank(message = "reasonCode不能为空")
    private String reasonCode;

    private String description;

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
