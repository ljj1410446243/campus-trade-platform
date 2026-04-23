package com.campus.trade.notice.controller;

import com.campus.trade.common.response.ApiResponse;
import com.campus.trade.notice.dto.CreateInternalNotificationRequest;
import com.campus.trade.notice.dto.NotificationResponse;
import com.campus.trade.notice.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {

    private final NotificationService notificationService;

    public InternalNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping({"", "/"})
    public ApiResponse<NotificationResponse> createNotification(@Valid @RequestBody CreateInternalNotificationRequest request) {
        return ApiResponse.success(notificationService.createNotification(request));
    }
}
