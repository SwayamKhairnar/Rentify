package com.rentify.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentify.auth.security.JwtService;
import com.rentify.item.Item;
import com.rentify.item.ItemCategory;
import com.rentify.item.ItemCondition;
import com.rentify.item.ItemRepository;
import com.rentify.notification.NotificationRepository;
import com.rentify.rental.Rental;
import com.rentify.rental.RentalRepository;
import com.rentify.rental.RentalStatus;
import com.rentify.review.dto.CreateReviewRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User owner;
    private User renter;
    private User thirdParty;
    private Item guitarItem;
    private Rental completedRental;
    private Rental pendingRental;

    @BeforeEach
    void setUp() {
        owner = new User("Owner Lisa", "lisa@example.com", passwordEncoder.encode("password123"), "North Campus");
        owner = userRepository.save(owner);

        renter = new User("Renter Mark", "mark@example.com", passwordEncoder.encode("password123"), "South Campus");
        renter = userRepository.save(renter);

        thirdParty = new User("Third Party", "third@example.com", passwordEncoder.encode("password123"), "West Campus");
        thirdParty = userRepository.save(thirdParty);

        guitarItem = new Item();
        guitarItem.setOwner(owner);
        guitarItem.setTitle("Fender Stratocaster Electric Guitar");
        guitarItem.setDescription("Classic electric guitar with amplifier");
        guitarItem.setCategory(ItemCategory.INSTRUMENTS);
        guitarItem.setPricePerDay(new BigDecimal("150.00"));
        guitarItem.setCondition(ItemCondition.LIKE_NEW);
        guitarItem.setLocation("Music Room");
        guitarItem.setAvailable(true);
        guitarItem = itemRepository.save(guitarItem);

        completedRental = new Rental();
        completedRental.setItem(guitarItem);
        completedRental.setOwner(owner);
        completedRental.setRenter(renter);
        completedRental.setStartDate(LocalDate.now().minusDays(5));
        completedRental.setEndDate(LocalDate.now().minusDays(2));
        completedRental.setTotalPrice(new BigDecimal("600.00"));
        completedRental.setStatus(RentalStatus.COMPLETED);
        completedRental = rentalRepository.save(completedRental);

        pendingRental = new Rental();
        pendingRental.setItem(guitarItem);
        pendingRental.setOwner(owner);
        pendingRental.setRenter(renter);
        pendingRental.setStartDate(LocalDate.now().plusDays(1));
        pendingRental.setEndDate(LocalDate.now().plusDays(3));
        pendingRental.setTotalPrice(new BigDecimal("450.00"));
        pendingRental.setStatus(RentalStatus.PENDING);
        pendingRental = rentalRepository.save(pendingRental);
    }

    @Test
    void testCreateReviewRenterToLenderSuccess() throws Exception {
        String token = jwtService.generateToken(renter.getId());

        CreateReviewRequest request = new CreateReviewRequest(
                completedRental.getId(),
                5,
                5,
                "Amazing guitar and wonderful lender!"
        );

        mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Review submitted successfully"))
                .andExpect(jsonPath("$.data.review.type").value("lender"))
                .andExpect(jsonPath("$.data.review.rating").value(5))
                .andExpect(jsonPath("$.data.review.itemRating").value(5))
                .andExpect(jsonPath("$.data.review.reviewer.name").value("Renter Mark"))
                .andExpect(jsonPath("$.data.review.targetUser.name").value("Owner Lisa"))
                .andExpect(jsonPath("$.data.review.item.title").value("Fender Stratocaster Electric Guitar"));

        User updatedOwner = userRepository.findById(owner.getId()).orElseThrow();
        assertThat(updatedOwner.getLenderRating()).isEqualByComparingTo("5.0");
        assertThat(updatedOwner.getTotalLenderReviews()).isEqualTo(1);
        assertThat(updatedOwner.getRating()).isEqualByComparingTo("5.0");
        assertThat(updatedOwner.getItemQualityAverage()).isEqualByComparingTo("5.0");

        Item updatedItem = itemRepository.findById(guitarItem.getId()).orElseThrow();
        assertThat(updatedItem.getRating()).isEqualByComparingTo("5.0");
        assertThat(updatedItem.getTotalReviews()).isEqualTo(1);

        assertThat(notificationRepository.countByRecipientIdAndIsReadFalse(owner.getId())).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testCreateReviewLenderToRenterSuccess() throws Exception {
        String token = jwtService.generateToken(owner.getId());

        CreateReviewRequest request = new CreateReviewRequest(
                completedRental.getId(),
                4,
                null,
                "Good renter, returned on time."
        );

        mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.review.type").value("renter"))
                .andExpect(jsonPath("$.data.review.rating").value(4))
                .andExpect(jsonPath("$.data.review.item").doesNotExist())
                .andExpect(jsonPath("$.data.review.reviewer.name").value("Owner Lisa"))
                .andExpect(jsonPath("$.data.review.targetUser.name").value("Renter Mark"));

        User updatedRenter = userRepository.findById(renter.getId()).orElseThrow();
        assertThat(updatedRenter.getRenterRating()).isEqualByComparingTo("4.0");
        assertThat(updatedRenter.getTotalRenterReviews()).isEqualTo(1);
    }

    @Test
    void testCreateReviewDuplicateBlocked() throws Exception {
        String token = jwtService.generateToken(renter.getId());

        CreateReviewRequest request = new CreateReviewRequest(completedRental.getId(), 5, 5, "First review");

        mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second review attempt
        mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You have already reviewed this rental"));
    }

    @Test
    void testCreateReviewIncompleteRentalBlocked() throws Exception {
        String token = jwtService.generateToken(renter.getId());

        CreateReviewRequest request = new CreateReviewRequest(pendingRental.getId(), 5, 5, "Premature review");

        mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Can only review completed rentals"));
    }

    @Test
    void testCreateReviewUnrelatedUserForbidden() throws Exception {
        String token = jwtService.generateToken(thirdParty.getId());

        CreateReviewRequest request = new CreateReviewRequest(completedRental.getId(), 5, 5, "Third party review");

        mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testGetReviewsByUserAndByRental() throws Exception {
        String token = jwtService.generateToken(renter.getId());

        CreateReviewRequest request = new CreateReviewRequest(completedRental.getId(), 5, 5, "Public review");
        mockMvc.perform(post("/api/reviews")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 1. Get reviews by user
        mockMvc.perform(get("/api/reviews/user/" + owner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pagination.total").value(1))
                .andExpect(jsonPath("$.data[0].rating").value(5))
                .andExpect(jsonPath("$.data[0].targetUser.name").value("Owner Lisa"));

        // 2. Get reviews by rental
        mockMvc.perform(get("/api/reviews/rental/" + completedRental.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviews.length()").value(1));
    }
}
