package com.campus.trade.notice.service;

import com.campus.trade.notice.dto.CreateInternalNotificationRequest;
import com.campus.trade.notice.dto.NotificationResponse;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse> listNotifications(String recipientUserId, String status);

    NotificationResponse createNotification(CreateInternalNotificationRequest request);

    void markRead(String recipientUserId, String notificationId);

    void markAllRead(String recipientUserId);
}
