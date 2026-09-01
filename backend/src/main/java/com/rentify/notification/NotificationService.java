package com.rentify.notification;

import com.rentify.exception.NotFoundException;
import com.rentify.notification.dto.NotificationResponse;
import com.rentify.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void createNotification(
            User recipient,
            User sender,
            NotificationType type,
            String title,
            String message,
            String link
    ) {
        if (recipient == null) {
            return;
        }

        Notification notification = new Notification(
                recipient,
                sender,
                type,
                title,
                message,
                link != null ? link : ""
        );

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findTop50ByRecipientIdOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));

        notification.setRead(true);
        Notification updated = notificationRepository.save(notification);
        return NotificationResponse.fromEntity(updated);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }

    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));

        notificationRepository.delete(notification);
    }
}
