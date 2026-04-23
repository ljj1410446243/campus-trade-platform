package com.campus.trade.notice.repository;

import com.campus.trade.notice.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId);

    List<Notification> findByRecipientUserIdAndIsReadOrderByCreatedAtDesc(String recipientUserId, boolean isRead);

    List<Notification> findByRecipientUserIdAndIsRead(String recipientUserId, boolean isRead);

    Optional<Notification> findByIdAndRecipientUserId(String id, String recipientUserId);
}
