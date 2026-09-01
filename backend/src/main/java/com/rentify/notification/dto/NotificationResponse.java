package com.rentify.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentify.notification.Notification;
import com.rentify.notification.NotificationType;
import com.rentify.review.dto.ReviewAuthorSummaryResponse;

import java.time.Instant;

public record NotificationResponse(
    Long id,
    ReviewAuthorSummaryResponse recipient,
    ReviewAuthorSummaryResponse sender,
    NotificationType type,
    String title,
    String message,
    String link,
    boolean isRead,
    Instant createdAt
) {
    @JsonProperty("_id")
    public Long getMongoId() {
        return id;
    }

    public static NotificationResponse fromEntity(Notification notification) {
        if (notification == null) return null;
        return new NotificationResponse(
            notification.getId(),
            ReviewAuthorSummaryResponse.fromEntity(notification.getRecipient()),
            ReviewAuthorSummaryResponse.fromEntity(notification.getSender()),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getLink(),
            notification.isRead(),
            notification.getCreatedAt()
        );
    }
}
