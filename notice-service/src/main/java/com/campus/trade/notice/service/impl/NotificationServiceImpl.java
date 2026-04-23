package com.campus.trade.notice.service.impl;

import com.campus.trade.notice.dto.CreateInternalNotificationRequest;
import com.campus.trade.notice.dto.NotificationResponse;
import com.campus.trade.notice.exception.BusinessException;
import com.campus.trade.notice.model.Notification;
import com.campus.trade.notice.repository.NotificationRepository;
import com.campus.trade.notice.service.NotificationService;
import com.campus.trade.notice.util.UserAccessGuard;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final String STATUS_UNREAD = "UNREAD";
    private static final String STATUS_READ = "READ";

    private final NotificationRepository notificationRepository;
    private final UserAccessGuard userAccessGuard;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UserAccessGuard userAccessGuard) {
        this.notificationRepository = notificationRepository;
        this.userAccessGuard = userAccessGuard;
    }

    @Override
    public List<NotificationResponse> listNotifications(String recipientUserId, String status) {
        userAccessGuard.requireUser(recipientUserId);

        List<Notification> notifications;
        if (status == null || status.isBlank()) {
            notifications = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId);
        } else if (STATUS_UNREAD.equalsIgnoreCase(status)) {
            notifications = notificationRepository.findByRecipientUserIdAndIsReadOrderByCreatedAtDesc(recipientUserId, false);
        } else if (STATUS_READ.equalsIgnoreCase(status)) {
            notifications = notificationRepository.findByRecipientUserIdAndIsReadOrderByCreatedAtDesc(recipientUserId, true);
        } else {
            throw new BusinessException(400, "status仅支持UNREAD或READ");
        }
        return notifications.stream().map(this::toResponse).toList();
    }

    @Override
    public NotificationResponse createNotification(CreateInternalNotificationRequest request) {
        userAccessGuard.requireUser(trimToRequired(request.getRecipientUserId(), "recipientUserId不能为空"));

        Notification notification = new Notification();
        notification.setRecipientUserId(request.getRecipientUserId().trim());
        notification.setCategory(trimToRequired(request.getCategory(), "category不能为空").toUpperCase(Locale.ROOT));
        notification.setTitle(trimToRequired(request.getTitle(), "title不能为空"));
        notification.setContent(trimToRequired(request.getContent(), "content不能为空"));
        notification.setActionType(trimToNull(request.getActionType()));
        notification.setActionTargetId(trimToNull(request.getActionTargetId()));
        notification.setExtra(request.getExtra());
        notification.setRead(false);
        notification.setReadAt(null);
        notification.setCreatedAt(new Date());

        return toResponse(notificationRepository.save(notification));
    }

    @Override
    public void markRead(String recipientUserId, String notificationId) {
        userAccessGuard.requireUser(recipientUserId);
        Notification notification = notificationRepository.findByIdAndRecipientUserId(notificationId, recipientUserId)
                .orElseThrow(() -> new BusinessException(404, "通知不存在"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(new Date());
            notificationRepository.save(notification);
        }
    }

    @Override
    public void markAllRead(String recipientUserId) {
        userAccessGuard.requireUser(recipientUserId);
        List<Notification> unreadNotifications = notificationRepository.findByRecipientUserIdAndIsRead(recipientUserId, false);
        if (unreadNotifications.isEmpty()) {
            return;
        }

        Date now = new Date();
        unreadNotifications.forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(now);
        });
        notificationRepository.saveAll(unreadNotifications);
    }

    private String trimToRequired(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException(400, message);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setNotificationId(notification.getId());
        response.setRecipientUserId(notification.getRecipientUserId());
        response.setCategory(notification.getCategory());
        response.setTitle(notification.getTitle());
        response.setContent(notification.getContent());
        response.setRead(notification.isRead());
        response.setCreatedAt(notification.getCreatedAt());
        response.setActionType(notification.getActionType());
        response.setActionTargetId(notification.getActionTargetId());
        response.setExtra(notification.getExtra());
        return response;
    }
}
