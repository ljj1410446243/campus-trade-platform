package com.campus.trade.user.service;

import com.campus.trade.user.dto.BanUserRequest;
import com.campus.trade.user.dto.CreateReportRequest;
import com.campus.trade.user.dto.ReportItemResponse;
import com.campus.trade.user.dto.ReviewReportRequest;

import java.util.List;

public interface ReportService {

    String createReport(String reporterId, CreateReportRequest request);

    List<ReportItemResponse> listMyReports(String reporterId);

    List<ReportItemResponse> listReports(String adminUserId, String status);

    ReportItemResponse getReportDetail(String adminUserId, String reportId);

    ReportItemResponse approveReport(String adminUserId, String reportId, ReviewReportRequest request);

    ReportItemResponse rejectReport(String adminUserId, String reportId, ReviewReportRequest request);

    void banUser(String adminUserId, String targetUserId, BanUserRequest request);

    void unbanUser(String adminUserId, String targetUserId);

    void offShelfItem(String adminUserId, String itemId);
}
