package com.campus.trade.user.controller;

import com.campus.trade.common.response.ApiResponse;
import com.campus.trade.user.dto.BanUserRequest;
import com.campus.trade.user.dto.ReportItemResponse;
import com.campus.trade.user.dto.ReviewReportRequest;
import com.campus.trade.user.service.ReportService;
import com.campus.trade.user.util.LoginUserHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final ReportService reportService;
    private final LoginUserHelper loginUserHelper;

    public AdminController(ReportService reportService, LoginUserHelper loginUserHelper) {
        this.reportService = reportService;
        this.loginUserHelper = loginUserHelper;
    }

    @GetMapping({"/reports", "/reports/"})
    public ApiResponse<List<ReportItemResponse>> listReports(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(required = false) String status) {

        return ApiResponse.success(reportService.listReports(loginUserHelper.getCurrentUserId(authorizationHeader), status));
    }

    @GetMapping({"/reports/{reportId}", "/reports/{reportId}/"})
    public ApiResponse<ReportItemResponse> getReportDetail(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String reportId) {

        return ApiResponse.success(reportService.getReportDetail(loginUserHelper.getCurrentUserId(authorizationHeader), reportId));
    }

    @PostMapping({"/reports/{reportId}/approve", "/reports/{reportId}/approve/"})
    public ApiResponse<ReportItemResponse> approveReport(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String reportId,
            @RequestBody(required = false) ReviewReportRequest request) {

        return ApiResponse.success(
                reportService.approveReport(loginUserHelper.getCurrentUserId(authorizationHeader), reportId, request)
        );
    }

    @PostMapping({"/reports/{reportId}/reject", "/reports/{reportId}/reject/"})
    public ApiResponse<ReportItemResponse> rejectReport(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String reportId,
            @RequestBody(required = false) ReviewReportRequest request) {

        return ApiResponse.success(
                reportService.rejectReport(loginUserHelper.getCurrentUserId(authorizationHeader), reportId, request)
        );
    }

    @PostMapping({"/users/{userId}/ban", "/users/{userId}/ban/"})
    @PutMapping({"/users/{userId}/ban", "/users/{userId}/ban/"})
    @PatchMapping({"/users/{userId}/ban", "/users/{userId}/ban/"})
    public ApiResponse<Void> banUser(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String userId,
            @RequestBody(required = false) BanUserRequest request) {

        reportService.banUser(loginUserHelper.getCurrentUserId(authorizationHeader), userId, request);
        return ApiResponse.success();
    }

    @PostMapping({"/users/{userId}/unban", "/users/{userId}/unban/"})
    @PutMapping({"/users/{userId}/unban", "/users/{userId}/unban/"})
    @PatchMapping({"/users/{userId}/unban", "/users/{userId}/unban/"})
    public ApiResponse<Void> unbanUser(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String userId) {

        reportService.unbanUser(loginUserHelper.getCurrentUserId(authorizationHeader), userId);
        return ApiResponse.success();
    }

    @PutMapping({"/items/{itemId}/off-shelf", "/items/{itemId}/off-shelf/"})
    public ApiResponse<Void> offShelfItem(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String itemId) {

        reportService.offShelfItem(loginUserHelper.getCurrentUserId(authorizationHeader), itemId);
        return ApiResponse.success();
    }
}
