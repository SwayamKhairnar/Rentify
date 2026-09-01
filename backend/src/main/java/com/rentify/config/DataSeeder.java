package com.rentify.config;

import com.rentify.conversation.Conversation;
import com.rentify.conversation.ConversationRepository;
import com.rentify.conversation.Message;
import com.rentify.conversation.MessageRepository;
import com.rentify.item.Item;
import com.rentify.item.ItemCategory;
import com.rentify.item.ItemCondition;
import com.rentify.item.ItemRepository;
import com.rentify.notification.Notification;
import com.rentify.notification.NotificationRepository;
import com.rentify.notification.NotificationType;
import com.rentify.rental.Rental;
import com.rentify.rental.RentalRepository;
import com.rentify.rental.RentalStatus;
import com.rentify.report.*;
import com.rentify.review.RatingAggregationService;
import com.rentify.review.Review;
import com.rentify.review.ReviewRepository;
import com.rentify.review.ReviewType;
import com.rentify.user.User;
import com.rentify.user.UserRepository;
import com.rentify.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final RentalRepository rentalRepository;
    private final ReviewRepository reviewRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final ReportRepository reportRepository;
    private final RatingAggregationService ratingAggregationService;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            UserRepository userRepository,
            ItemRepository itemRepository,
            RentalRepository rentalRepository,
            ReviewRepository reviewRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            NotificationRepository notificationRepository,
            ReportRepository reportRepository,
            RatingAggregationService ratingAggregationService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.rentalRepository = rentalRepository;
        this.reviewRepository = reviewRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
        this.reportRepository = reportRepository;
        this.ratingAggregationService = ratingAggregationService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains data. Skipping initial seeding.");
            return;
        }

        log.info("Starting initial seed data insertion...");

        // 1. Users
        String encodedPassword = passwordEncoder.encode("password123");

        User admin = new User("Admin User", "admin@example.com", encodedPassword, "Admin HQ");
        admin.setRole(UserRole.ADMIN);
        admin.setBio("Platform Administrator");
        admin.setAvatar("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150");
        admin = userRepository.save(admin);

        User john = new User("John Doe", "john@example.com", encodedPassword, "North Campus");
        john.setPhone("+1234567890");
        john.setBio("Photography enthusiast & CS senior.");
        john.setAvatar("https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150");
        john = userRepository.save(john);

        User jane = new User("Jane Smith", "jane@example.com", encodedPassword, "South Campus");
        jane.setPhone("+1987654321");
        jane.setBio("Biology major, love sharing gear and books!");
        jane.setAvatar("https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150");
        jane = userRepository.save(jane);

        User sarah = new User("Sarah Jenkins", "sarah@example.com", encodedPassword, "Engineering Block");
        sarah.setPhone("+1555123456");
        sarah.setBio("Robotics & outdoor adventurer.");
        sarah.setAvatar("https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150");
        sarah = userRepository.save(sarah);

        // 2. Items
        Item camera = createItem(john, "Sony Alpha A7 III Full-Frame Mirrorless",
                "Includes 28-70mm lens, 2 batteries, and 64GB SD card. Excellent 4K video and photo quality.",
                ItemCategory.CAMERAS, new BigDecimal("45.00"), ItemCondition.LIKE_NEW, "North Campus Library",
                List.of("https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=600"));

        Item calculator = createItem(jane, "Texas Instruments TI-84 Plus CE Graphing Calculator",
                "Color screen, rechargeable battery, preloaded with useful math & engineering apps.",
                ItemCategory.ELECTRONICS, new BigDecimal("5.00"), ItemCondition.GOOD, "Science Complex",
                List.of("https://images.unsplash.com/photo-1594980596870-8aa52a78d8cd?w=600"));

        Item bike = createItem(sarah, "Trek FX 2 Disc Road & Commuter Bike",
                "Size Medium (fits 5'5 to 5'10). Disc brakes, 18-speed Shimano drivetrain, helmet and lock included.",
                ItemCategory.BIKES, new BigDecimal("20.00"), ItemCondition.GOOD, "West Dorms Bike Rack",
                List.of("https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=600"));

        Item textbook = createItem(jane, "Organic Chemistry 8th Edition - Paula Yurkanis Bruice",
                "Hardcover textbook with clear illustrations. No missing pages or heavy highlighting.",
                ItemCategory.TEXTBOOKS, new BigDecimal("8.00"), ItemCondition.GOOD, "South Dining Hall",
                List.of("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600"));

        Item guitar = createItem(john, "Yamaha FG800 Solid Top Acoustic Guitar",
                "Rich acoustic tone, comes with padded gig bag, clip-on tuner, and extra picks.",
                ItemCategory.INSTRUMENTS, new BigDecimal("18.00"), ItemCondition.LIKE_NEW, "Music Practice Rooms",
                List.of("https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=600"));

        // 3. Rentals
        // 3a. Completed Rental: Jane rented camera from John
        Rental completedRental = new Rental();
        completedRental.setItem(camera);
        completedRental.setOwner(john);
        completedRental.setRenter(jane);
        completedRental.setStartDate(LocalDate.now().minusDays(5));
        completedRental.setEndDate(LocalDate.now().minusDays(2));
        completedRental.setTotalPrice(new BigDecimal("180.00"));
        completedRental.setStatus(RentalStatus.COMPLETED);
        completedRental.setMessage("Need it for my weekend photography project!");
        completedRental = rentalRepository.save(completedRental);

        // 3b. Active Rental: Sarah renting calculator from Jane
        Rental activeRental = new Rental();
        activeRental.setItem(calculator);
        activeRental.setOwner(jane);
        activeRental.setRenter(sarah);
        activeRental.setStartDate(LocalDate.now().minusDays(1));
        activeRental.setEndDate(LocalDate.now().plusDays(2));
        activeRental.setTotalPrice(new BigDecimal("20.00"));
        activeRental.setStatus(RentalStatus.ACTIVE);
        activeRental.setMessage("Midterm exams preparation.");
        activeRental = rentalRepository.save(activeRental);

        // 3c. Approved Rental: John renting bike from Sarah
        Rental approvedRental = new Rental();
        approvedRental.setItem(bike);
        approvedRental.setOwner(sarah);
        approvedRental.setRenter(john);
        approvedRental.setStartDate(LocalDate.now().plusDays(1));
        approvedRental.setEndDate(LocalDate.now().plusDays(4));
        approvedRental.setTotalPrice(new BigDecimal("80.00"));
        approvedRental.setStatus(RentalStatus.APPROVED);
        approvedRental.setMessage("Weekend trail ride with friends.");
        approvedRental = rentalRepository.save(approvedRental);

        // 3d. Pending Rental: Jane requesting guitar from John
        Rental pendingRental = new Rental();
        pendingRental.setItem(guitar);
        pendingRental.setOwner(john);
        pendingRental.setRenter(jane);
        pendingRental.setStartDate(LocalDate.now().plusDays(3));
        pendingRental.setEndDate(LocalDate.now().plusDays(6));
        pendingRental.setTotalPrice(new BigDecimal("72.00"));
        pendingRental.setStatus(RentalStatus.PENDING);
        pendingRental.setMessage("Looking forward to practicing for the campus open mic!");
        pendingRental = rentalRepository.save(pendingRental);

        // 4. Conversations & Messages
        Conversation conv1 = new Conversation(completedRental, jane, john);
        conv1.setLastMessage("Thanks for returning it on time! Hope the photos came out great.");
        conv1.setLastMessageAt(Instant.now().minusSeconds(86400));
        conv1 = conversationRepository.save(conv1);

        messageRepository.save(new Message(conv1, jane, "Hi John, is the camera battery fully charged?"));
        messageRepository.save(new Message(conv1, john, "Yes, both batteries are 100% charged and ready!"));
        messageRepository.save(new Message(conv1, john, "Thanks for returning it on time! Hope the photos came out great."));

        Conversation conv2 = new Conversation(activeRental, sarah, jane);
        conv2.setLastMessage("All set, thank you so much!");
        conv2.setLastMessageAt(Instant.now().minusSeconds(3600));
        conv2 = conversationRepository.save(conv2);

        messageRepository.save(new Message(conv2, sarah, "Hi Jane, I just picked up the calculator at the science library."));
        messageRepository.save(new Message(conv2, jane, "Great, let me know if you need help with any formulas!"));
        messageRepository.save(new Message(conv2, sarah, "All set, thank you so much!"));

        // 5. Reviews
        Review review1 = new Review();
        review1.setRental(completedRental);
        review1.setReviewer(jane);
        review1.setReviewee(john);
        review1.setType(ReviewType.LENDER);
        review1.setRating(5);
        review1.setItemRating(5);
        review1.setComment("Amazing camera! Everything was packaged securely and John was super helpful.");
        reviewRepository.save(review1);

        Review review2 = new Review();
        review2.setRental(completedRental);
        review2.setReviewer(john);
        review2.setReviewee(jane);
        review2.setType(ReviewType.RENTER);
        review2.setRating(5);
        review2.setComment("Jane was wonderful to deal with. Returned the equipment in flawless shape.");
        reviewRepository.save(review2);

        ratingAggregationService.recalculateUserRatings(john.getId());
        ratingAggregationService.recalculateUserRatings(jane.getId());
        ratingAggregationService.recalculateItemRatings(camera.getId());

        // 6. Reports & Notifications
        Report sampleReport = new Report();
        sampleReport.setReporter(sarah);
        sampleReport.setReportedUser(john);
        sampleReport.setRental(completedRental);
        sampleReport.setReason(ReportReason.OTHER);
        sampleReport.setDescription("Sample dispute test report for administrative review.");
        sampleReport.setStatus(ReportStatus.PENDING);
        sampleReport.setAdminAction(AdminAction.NONE);
        reportRepository.save(sampleReport);

        notificationRepository.save(new Notification(
                jane,
                john,
                NotificationType.RENTAL_STATUS,
                "Rental Request Approved",
                "Your request to rent Sony Alpha A7 III has been approved!",
                "/rentals/" + completedRental.getId()
        ));

        notificationRepository.save(new Notification(
                john,
                jane,
                NotificationType.REVIEW_RECEIVED,
                "New Review Received",
                "Jane Smith left you a 5-star review.",
                "/users/" + john.getId()
        ));

        log.info("Initial seed data inserted successfully! Seeded 4 users, 5 items, 4 rentals, 2 conversations, 2 reviews, 1 report.");
    }

    private Item createItem(User owner, String title, String description, ItemCategory category,
                            BigDecimal pricePerDay, ItemCondition condition, String location, List<String> images) {
        Item item = new Item();
        item.setOwner(owner);
        item.setTitle(title);
        item.setDescription(description);
        item.setCategory(category);
        item.setPricePerDay(pricePerDay);
        item.setCondition(condition);
        item.setLocation(location);
        item.setAvailable(true);
        if (images != null) {
            images.forEach(item::addImage);
        }
        return itemRepository.save(item);
    }
}
