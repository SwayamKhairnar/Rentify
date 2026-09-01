package com.rentify.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentify.auth.security.JwtService;
import com.rentify.conversation.dto.SendMessageRequest;
import com.rentify.item.Item;
import com.rentify.item.ItemCategory;
import com.rentify.item.ItemCondition;
import com.rentify.item.ItemRepository;
import com.rentify.notification.NotificationRepository;
import com.rentify.rental.Rental;
import com.rentify.rental.RentalRepository;
import com.rentify.rental.RentalStatus;
import com.rentify.user.User;
import com.rentify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User user1;
    private User user2;
    private User user3;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        user1 = new User("User One", "user1@example.com", passwordEncoder.encode("password123"), "North Campus");
        user1 = userRepository.save(user1);

        user2 = new User("User Two", "user2@example.com", passwordEncoder.encode("password123"), "South Campus");
        user2 = userRepository.save(user2);

        user3 = new User("User Three", "user3@example.com", passwordEncoder.encode("password123"), "West Campus");
        user3 = userRepository.save(user3);

        Item item = new Item();
        item.setOwner(user1);
        item.setTitle("Monitor");
        item.setDescription("27 inch 4K monitor");
        item.setCategory(ItemCategory.ELECTRONICS);
        item.setPricePerDay(new BigDecimal("50.00"));
        item.setCondition(ItemCondition.LIKE_NEW);
        item = itemRepository.save(item);

        Rental rental = new Rental();
        rental.setItem(item);
        rental.setOwner(user1);
        rental.setRenter(user2);
        rental.setStartDate(LocalDate.now().plusDays(1));
        rental.setEndDate(LocalDate.now().plusDays(3));
        rental.setTotalPrice(new BigDecimal("150.00"));
        rental.setStatus(RentalStatus.APPROVED);
        rental = rentalRepository.save(rental);

        conversation = new Conversation(rental, user1, user2);
        conversation = conversationRepository.save(conversation);
    }

    @Test
    void testGetConversations() throws Exception {
        String token = jwtService.generateToken(user1.getId());

        mockMvc.perform(get("/api/chat")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.conversations.length()").value(1))
                .andExpect(jsonPath("$.data.conversations[0]._id").value(conversation.getId()));
    }

    @Test
    void testSendMessageAndReceiveNotification() throws Exception {
        String token = jwtService.generateToken(user1.getId());

        SendMessageRequest request = new SendMessageRequest(
                conversation.getId(),
                "Hi! Where should we meet for handover?"
        );

        mockMvc.perform(post("/api/chat/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Message sent"))
                .andExpect(jsonPath("$.data.message.content").value("Hi! Where should we meet for handover?"))
                .andExpect(jsonPath("$.data.message.sender.name").value("User One"));

        // Verify recipient unread count is 1
        String user2Token = jwtService.generateToken(user2.getId());
        mockMvc.perform(get("/api/chat/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));

        // Verify notification was sent to user2
        assertThat(notificationRepository.countByRecipientIdAndIsReadFalse(user2.getId())).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testGetMessagesMarksAsRead() throws Exception {
        // Send a message from user1
        Message message = new Message(conversation, user1, "Hello from user 1");
        messageRepository.save(message);

        String user2Token = jwtService.generateToken(user2.getId());

        // Fetch messages as user2
        mockMvc.perform(get("/api/chat/" + conversation.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messages.length()").value(1))
                .andExpect(jsonPath("$.data.messages[0].content").value("Hello from user 1"));

        // Verify unread count is now 0 for user2
        mockMvc.perform(get("/api/chat/unread")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0));
    }

    @Test
    void testNonParticipantSendMessageForbidden() throws Exception {
        String token = jwtService.generateToken(user3.getId());

        mockMvc.perform(post("/api/chat/" + conversation.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Unauthorized intrusion"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
