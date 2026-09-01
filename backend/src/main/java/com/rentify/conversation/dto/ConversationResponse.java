package com.rentify.conversation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentify.conversation.Conversation;
import com.rentify.rental.dto.RentalResponse;
import com.rentify.review.dto.ReviewAuthorSummaryResponse;

import java.time.Instant;
import java.util.List;

public record ConversationResponse(
    Long id,
    RentalResponse rental,
    List<ReviewAuthorSummaryResponse> participants,
    String lastMessage,
    Instant lastMessageAt,
    long unreadCount,
    Instant createdAt,
    Instant updatedAt
) {
    @JsonProperty("_id")
    public Long getMongoId() {
        return id;
    }

    public static ConversationResponse fromEntity(Conversation conv, long unreadCount) {
        if (conv == null) return null;
        List<ReviewAuthorSummaryResponse> participantList = conv.getParticipants() != null
                ? conv.getParticipants().stream().map(ReviewAuthorSummaryResponse::fromEntity).toList()
                : List.of();

        return new ConversationResponse(
            conv.getId(),
            conv.getRental() != null ? RentalResponse.fromEntity(conv.getRental()) : null,
            participantList,
            conv.getLastMessage(),
            conv.getLastMessageAt(),
            unreadCount,
            conv.getCreatedAt(),
            conv.getUpdatedAt()
        );
    }
}
