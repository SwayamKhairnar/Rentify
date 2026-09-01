package com.rentify.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
    @NotNull(message = "Conversation ID is required")
    Long conversationId,

    @NotBlank(message = "Message content is required")
    @Size(max = 2000, message = "Message cannot exceed 2000 characters")
    String content
) {}
