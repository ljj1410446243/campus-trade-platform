package com.campus.trade.user.controller;

import com.campus.trade.common.response.ApiResponse;
import com.campus.trade.user.dto.CreateReportRequest;
import com.campus.trade.user.dto.ReportItemResponse;
import com.campus.trade.user.service.ReportService;
import com.campus.trade.user.util.LoginUserHelper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final LoginUserHelper loginUserHelper;

    public ReportController(ReportService reportService, LoginUserHelper loginUserHelper) {
        this.reportService = reportService;
        this.loginUserHelper = loginUserHelper;
    }

    @PostMapping({"", "/"})
    public ApiResponse<Map<String, String>> createReport(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateReportRequest request) {

        String userId = loginUserHelper.getCurrentUserId(authorizationHeader);
        return ApiResponse.success(Map.of("reportId", reportService.createReport(userId, request)));
    }

    @GetMapping({"/mine", "/mine/"})
    public ApiResponse<List<ReportItemResponse>> listMyReports(
            @RequestHeader("Authorization") String authorizationHeader) {

        String userId = loginUserHelper.getCurrentUserId(authorizationHeader);
        return ApiResponse.success(reportService.listMyReports(userId));
    }
}
