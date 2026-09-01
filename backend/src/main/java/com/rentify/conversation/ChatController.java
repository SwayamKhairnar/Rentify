package com.rentify.conversation;

import com.rentify.auth.security.CustomUserDetails;
import com.rentify.common.ApiResponse;
import com.rentify.common.CurrentUser;
import com.rentify.conversation.dto.ConversationResponse;
import com.rentify.conversation.dto.MessageResponse;
import com.rentify.conversation.dto.SendMessageRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, List<ConversationResponse>>>> getConversations(
            @CurrentUser CustomUserDetails userDetails
    ) {
        List<ConversationResponse> conversations = chatService.getConversations(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Conversations fetched", Map.of("conversations", conversations)));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<Map<String, List<ConversationResponse>>>> getConversationsAlias(
            @CurrentUser CustomUserDetails userDetails
    ) {
        return getConversations(userDetails);
    }

    @GetMapping({"/unread", "/unread-count"})
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @CurrentUser CustomUserDetails userDetails
    ) {
        long count = chatService.getUnreadCount(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Unread count fetched", Map.of("count", count, "unreadCount", count)));
    }

    @GetMapping({"/{conversationId}", "/conversations/{conversationId}/messages"})
    public ResponseEntity<ApiResponse<Map<String, List<MessageResponse>>>> getMessages(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long conversationId
    ) {
        List<MessageResponse> messages = chatService.getMessages(userDetails.getId(), conversationId);
        return ResponseEntity.ok(ApiResponse.success("Messages fetched", Map.of("messages", messages)));
    }

    @PostMapping("/{conversationId}")
    public ResponseEntity<ApiResponse<Map<String, MessageResponse>>> sendMessageInConversation(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> body
    ) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new com.rentify.exception.BadRequestException("Message content is required");
        }
        MessageResponse message = chatService.sendMessage(userDetails.getId(), conversationId, content);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Message sent", Map.of("message", message)));
    }

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<Map<String, MessageResponse>>> sendMessage(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody SendMessageRequest request
    ) {
        MessageResponse message = chatService.sendMessage(userDetails.getId(), request.conversationId(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Message sent", Map.of("message", message)));
    }
}
