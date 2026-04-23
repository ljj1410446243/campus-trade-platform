package com.campus.trade.notice.controller;

import com.campus.trade.common.response.ApiResponse;
import com.campus.trade.notice.dto.NotificationResponse;
import com.campus.trade.notice.service.NotificationService;
import com.campus.trade.notice.util.LoginUserHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final LoginUserHelper loginUserHelper;

    public NotificationController(NotificationService notificationService, LoginUserHelper loginUserHelper) {
        this.notificationService = notificationService;
        this.loginUserHelper = loginUserHelper;
    }

    @GetMapping({"", "/"})
    public ApiResponse<List<NotificationResponse>> listNotifications(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(required = false) String status) {

        String userId = loginUserHelper.getCurrentUserId(authorizationHeader);
        return ApiResponse.success(notificationService.listNotifications(userId, status));
    }

    @PostMapping({"/{notificationId}/read", "/{notificationId}/read/"})
    public ApiResponse<Void> markRead(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String notificationId) {

        String userId = loginUserHelper.getCurrentUserId(authorizationHeader);
        notificationService.markRead(userId, notificationId);
        return ApiResponse.success();
    }

    @PostMapping({"/read-all", "/read-all/"})
    public ApiResponse<Void> markAllRead(@RequestHeader("Authorization") String authorizationHeader) {
        String userId = loginUserHelper.getCurrentUserId(authorizationHeader);
        notificationService.markAllRead(userId);
        return ApiResponse.success();
    }
}
