package com.rentify.repository;

import com.rentify.conversation.*;
import com.rentify.item.*;
import com.rentify.notification.*;
import com.rentify.rental.*;
import com.rentify.report.*;
import com.rentify.review.*;
import com.rentify.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RepositoryMappingTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemImageRepository itemImageRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ReportRepository reportRepository;

    private User owner;
    private User renter;

    @BeforeEach
    void setUp() {
        owner = new User("Owner Student", "owner@example.com", "password123", "North Campus");
        renter = new User("Renter Student", "renter@example.com", "password123", "South Campus");
        owner = userRepository.save(owner);
        renter = userRepository.save(renter);
    }

    @Test
    void testUserPersistenceAndQuery() {
        Optional<User> found = userRepository.findByEmailIgnoreCase("OWNER@EXAMPLE.COM");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Owner Student");
        assertThat(found.get().getRole()).isEqualTo(UserRole.STUDENT);
        assertThat(found.get().getVersion()).isEqualTo(0L);
    }

    @Test
    void testItemAndImagePersistence() {
        Item item = new Item();
        item.setOwner(owner);
        item.setTitle("Canon EOS 1500D DSLR");
        item.setDescription("Excellent camera for lab and events");
        item.setCategory(ItemCategory.CAMERAS);
        item.setPricePerDay(new BigDecimal("450.00"));
        item.setCondition(ItemCondition.LIKE_NEW);
        item.setLocation("Hostel 4, Room 202");
        item.addImage("https://res.cloudinary.com/test/image/upload/v1/camera1.jpg");
        item.addImage("https://res.cloudinary.com/test/image/upload/v1/camera2.jpg");

        Item savedItem = itemRepository.save(item);
        assertThat(savedItem.getId()).isNotNull();
        assertThat(savedItem.getImages()).hasSize(2);

        List<Item> ownerItems = itemRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId());
        assertThat(ownerItems).hasSize(1);
        assertThat(ownerItems.get(0).getTitle()).isEqualTo("Canon EOS 1500D DSLR");
    }

    @Test
    void testRentalAndOverlapQuery() {
        Item item = new Item();
        item.setOwner(owner);
        item.setTitle("Mountain Bike 21 Gear");
        item.setDescription("Smooth riding bike");
        item.setCategory(ItemCategory.BIKES);
        item.setPricePerDay(new BigDecimal("150.00"));
        item = itemRepository.save(item);

        Rental rental = new Rental();
        rental.setItem(item);
        rental.setRenter(renter);
        rental.setOwner(owner);
        rental.setStartDate(LocalDate.of(2026, 9, 10));
        rental.setEndDate(LocalDate.of(2026, 9, 15));
        rental.setTotalPrice(new BigDecimal("750.00"));
        rental.setStatus(RentalStatus.APPROVED);
        rental = rentalRepository.save(rental);

        assertThat(rental.getId()).isNotNull();

        // Check overlap for intersecting dates (Sept 12 to Sept 14)
        List<Rental> overlapping = rentalRepository.findOverlappingRentals(
                item.getId(),
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 14),
                List.of(RentalStatus.APPROVED, RentalStatus.ACTIVE)
        );
        assertThat(overlapping).hasSize(1);

        // Check overlap for non-intersecting dates (Sept 16 to Sept 20)
        List<Rental> nonOverlapping = rentalRepository.findOverlappingRentals(
                item.getId(),
                LocalDate.of(2026, 9, 16),
                LocalDate.of(2026, 9, 20),
                List.of(RentalStatus.APPROVED, RentalStatus.ACTIVE)
        );
        assertThat(nonOverlapping).isEmpty();

        // Check hasRentalsWithStatuses
        boolean hasActiveOrApproved = itemRepository.hasRentalsWithStatuses(
                item.getId(),
                List.of(RentalStatus.APPROVED, RentalStatus.ACTIVE)
        );
        assertThat(hasActiveOrApproved).isTrue();
    }

    @Test
    void testConversationAndMessageFlow() {
        Item item = new Item();
        item.setOwner(owner);
        item.setTitle("Scientific Calculator TI-84");
        item.setDescription("Graphing calculator");
        item.setCategory(ItemCategory.ELECTRONICS);
        item.setPricePerDay(new BigDecimal("50.00"));
        item = itemRepository.save(item);

        Rental rental = new Rental();
        rental.setItem(item);
        rental.setRenter(renter);
        rental.setOwner(owner);
        rental.setStartDate(LocalDate.of(2026, 9, 5));
        rental.setEndDate(LocalDate.of(2026, 9, 7));
        rental.setTotalPrice(new BigDecimal("100.00"));
        rental = rentalRepository.save(rental);

        Conversation conversation = new Conversation(rental, owner, renter);
        conversation.setLastMessage("Hello, when can I pick it up?");
        conversation = conversationRepository.save(conversation);

        Message message = new Message(conversation, renter, "Hello, when can I pick it up?");
        messageRepository.save(message);

        long unreadForOwner = messageRepository.countUnreadForUser(owner.getId());
        assertThat(unreadForOwner).isEqualTo(1);

        long unreadForRenter = messageRepository.countUnreadForUser(renter.getId());
        assertThat(unreadForRenter).isEqualTo(0);

        int markedRead = messageRepository.markConversationMessagesRead(conversation.getId(), owner.getId());
        assertThat(markedRead).isEqualTo(1);

        unreadForOwner = messageRepository.countUnreadForUser(owner.getId());
        assertThat(unreadForOwner).isEqualTo(0);
    }

    @Test
    void testReviewNotificationAndReportPersistence() {
        Item item = new Item();
        item.setOwner(owner);
        item.setTitle("Engineering Thermodynamics Textbook");
        item.setDescription("Prescribed 8th edition");
        item.setCategory(ItemCategory.TEXTBOOKS);
        item.setPricePerDay(new BigDecimal("30.00"));
        item = itemRepository.save(item);

        Rental rental = new Rental();
        rental.setItem(item);
        rental.setRenter(renter);
        rental.setOwner(owner);
        rental.setStartDate(LocalDate.of(2026, 9, 1));
        rental.setEndDate(LocalDate.of(2026, 9, 3));
        rental.setTotalPrice(new BigDecimal("60.00"));
        rental.setStatus(RentalStatus.COMPLETED);
        rental = rentalRepository.save(rental);

        // Review
        Review review = new Review();
        review.setRental(rental);
        review.setReviewer(renter);
        review.setReviewee(owner);
        review.setRating(5);
        review.setItemRating(5);
        review.setType(ReviewType.LENDER);
        review.setComment("Great lender and perfect book condition.");
        review = reviewRepository.save(review);

        assertThat(review.getId()).isNotNull();
        assertThat(reviewRepository.existsByRentalIdAndReviewerId(rental.getId(), renter.getId())).isTrue();

        // Notification
        Notification notification = new Notification(
                owner,
                renter,
                NotificationType.REVIEW_RECEIVED,
                "New Review Received",
                "Renter left you a 5-star review!",
                "/reviews"
        );
        notification = notificationRepository.save(notification);
        assertThat(notification.getId()).isNotNull();

        long unreadNotifications = notificationRepository.countByRecipientIdAndIsReadFalse(owner.getId());
        assertThat(unreadNotifications).isEqualTo(1);

        // Report
        Report report = new Report();
        report.setReporter(owner);
        report.setReportedUser(renter);
        report.setRental(rental);
        report.setReason(ReportReason.LATE_RETURN);
        report.setDescription("Book was returned 2 days late without prior notice.");
        report.setStatus(ReportStatus.PENDING);
        report = reportRepository.save(report);

        assertThat(report.getId()).isNotNull();
        List<Report> reports = reportRepository.findAllByOrderByCreatedAtDesc();
        assertThat(reports).isNotEmpty();
    }
}
