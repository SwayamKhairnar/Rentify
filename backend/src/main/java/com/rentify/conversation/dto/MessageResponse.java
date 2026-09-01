package com.rentify.conversation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentify.conversation.Message;
import com.rentify.review.dto.ReviewAuthorSummaryResponse;

import java.time.Instant;

public record MessageResponse(
    Long id,
    Long conversationId,
    ReviewAuthorSummaryResponse sender,
    String content,
    boolean isRead,
    Instant createdAt
) {
    @JsonProperty("_id")
    public Long getMongoId() {
        return id;
    }

    public static MessageResponse fromEntity(Message message) {
        if (message == null) return null;
        return new MessageResponse(
            message.getId(),
            message.getConversation() != null ? message.getConversation().getId() : null,
            ReviewAuthorSummaryResponse.fromEntity(message.getSender()),
            message.getContent(),
            message.isRead(),
            message.getCreatedAt()
        );
    }
}
