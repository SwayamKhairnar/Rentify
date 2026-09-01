package com.rentify.conversation.dto;

import jakarta.validation.constraints.NotNull;

public record CreateConversationRequest(
    @NotNull(message = "Recipient ID is required")
    Long recipientId,

    Long itemId
) {}
