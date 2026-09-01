package com.rentify.notification;

import com.rentify.auth.security.JwtService;
import com.rentify.user.User;
import com.rentify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User recipient;
    private User sender;
    private Notification notif1;
    private Notification notif2;

    @BeforeEach
    void setUp() {
        recipient = new User("Recipient User", "recipient@example.com", passwordEncoder.encode("password123"), "North Campus");
        recipient = userRepository.save(recipient);

        sender = new User("Sender User", "sender@example.com", passwordEncoder.encode("password123"), "South Campus");
        sender = userRepository.save(sender);

        notif1 = new Notification(recipient, sender, NotificationType.RENTAL_REQUEST, "Rental Request", "Bob wants your bike", "/rentals/1");
        notif1 = notificationRepository.save(notif1);

        notif2 = new Notification(recipient, sender, NotificationType.MESSAGE, "New Message", "Hello there!", "/chat/1");
        notif2 = notificationRepository.save(notif2);
    }

    @Test
    void testGetNotificationsAndUnreadCount() throws Exception {
        String token = jwtService.generateToken(recipient.getId());

        mockMvc.perform(get("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notifications.length()").value(2));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2));
    }

    @Test
    void testMarkAsReadAndReadAll() throws Exception {
        String token = jwtService.generateToken(recipient.getId());

        // 1. Mark single notification read
        mockMvc.perform(patch("/api/notifications/" + notif1.getId() + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notification.isRead").value(true));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));

        // 2. Mark all read
        mockMvc.perform(patch("/api/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0));
    }

    @Test
    void testDeleteNotification() throws Exception {
        String token = jwtService.generateToken(recipient.getId());

        mockMvc.perform(delete("/api/notifications/" + notif1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Notification deleted"));

        assertThat(notificationRepository.findById(notif1.getId())).isEmpty();
    }
}
