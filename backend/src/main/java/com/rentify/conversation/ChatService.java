package com.rentify.conversation;

import com.rentify.conversation.dto.ConversationResponse;
import com.rentify.conversation.dto.MessageResponse;
import com.rentify.exception.ForbiddenException;
import com.rentify.exception.NotFoundException;
import com.rentify.notification.NotificationService;
import com.rentify.notification.NotificationType;
import com.rentify.user.User;
import com.rentify.user.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findUserConversations(userId);
        return conversations.stream()
                .map(c -> {
                    long unread = messageRepository.countUnreadInConversation(c.getId(), userId);
                    return ConversationResponse.fromEntity(c, unread);
                })
                .toList();
    }

    @Transactional
    public List<MessageResponse> getMessages(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        if (!conversationRepository.isParticipant(conversationId, userId)) {
            throw new ForbiddenException("You are not part of this conversation");
        }

        messageRepository.markMessagesAsRead(conversationId, userId);

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        return messages.stream()
                .map(MessageResponse::fromEntity)
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(Long senderId, Long conversationId, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!conversationRepository.isParticipant(conversationId, senderId)) {
            throw new ForbiddenException("You are not part of this conversation");
        }

        Message message = new Message(conversation, sender, content.trim());
        Message savedMessage = messageRepository.save(message);

        String snippet = content.length() > 100 ? content.substring(0, 97) + "..." : content;
        conversation.setLastMessage(snippet);
        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        User recipient = conversation.getParticipants().stream()
                .filter(p -> !p.getId().equals(senderId))
                .findFirst()
                .orElse(null);

        if (recipient != null) {
            String notifSnippet = content.length() > 50 ? content.substring(0, 47) + "..." : content;
            notificationService.createNotification(
                    recipient,
                    sender,
                    NotificationType.MESSAGE,
                    "New Message from " + sender.getName(),
                    notifSnippet,
                    "/chat/" + conversation.getId()
            );
        }

        MessageResponse response = MessageResponse.fromEntity(savedMessage);

        try {
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);
            if (recipient != null) {
                long unread = messageRepository.countUnreadInConversation(conversationId, recipient.getId());
                messagingTemplate.convertAndSend("/topic/users/" + recipient.getId() + "/unread", unread);
            }
        } catch (Exception e) {
            // Log but don't fail transactional message persistence if WebSocket push encounters client disconnect
        }

        return response;
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        List<Conversation> conversations = conversationRepository.findUserConversations(userId);
        if (conversations.isEmpty()) {
            return 0;
        }
        List<Long> conversationIds = conversations.stream().map(Conversation::getId).toList();
        return messageRepository.countUnreadMessages(conversationIds, userId);
    }
}
