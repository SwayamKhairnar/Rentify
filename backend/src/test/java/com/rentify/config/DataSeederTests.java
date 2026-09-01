package com.rentify.config;

import com.rentify.conversation.ConversationRepository;
import com.rentify.conversation.MessageRepository;
import com.rentify.item.ItemRepository;
import com.rentify.notification.NotificationRepository;
import com.rentify.rental.RentalRepository;
import com.rentify.report.ReportRepository;
import com.rentify.review.RatingAggregationService;
import com.rentify.review.ReviewRepository;
import com.rentify.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DataSeederTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private RatingAggregationService ratingAggregationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void testDataSeederRunsAndIsIdempotent() {
        reportRepository.deleteAll();
        reviewRepository.deleteAll();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        notificationRepository.deleteAll();
        rentalRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.deleteAll();

        DataSeeder seeder = new DataSeeder(
                userRepository,
                itemRepository,
                rentalRepository,
                reviewRepository,
                conversationRepository,
                messageRepository,
                notificationRepository,
                reportRepository,
                ratingAggregationService,
                passwordEncoder
        );

        // First run seeds data
        seeder.run();
        long userCount = userRepository.count();
        assertThat(userCount).isGreaterThanOrEqualTo(4);
        assertThat(itemRepository.count()).isGreaterThanOrEqualTo(5);

        // Second run is a no-op because data exists
        seeder.run();
        assertThat(userRepository.count()).isEqualTo(userCount);
    }
}
