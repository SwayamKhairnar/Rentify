package com.rentify.rental;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentify.auth.security.JwtService;
import com.rentify.item.Item;
import com.rentify.item.ItemCategory;
import com.rentify.item.ItemCondition;
import com.rentify.item.ItemRepository;
import com.rentify.notification.NotificationRepository;
import com.rentify.rental.dto.CreateRentalRequest;
import com.rentify.rental.dto.UpdateRentalStatusRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RentalControllerTests {

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
    private Item cameraItem;

    @BeforeEach
    void setUp() {
        owner = new User("Owner Alice", "owner@example.com", passwordEncoder.encode("password123"), "North Campus");
        owner = userRepository.save(owner);

        renter = new User("Renter Bob", "renter@example.com", passwordEncoder.encode("password123"), "South Campus");
        renter = userRepository.save(renter);

        thirdParty = new User("Charlie ThirdParty", "charlie@example.com", passwordEncoder.encode("password123"), "West Campus");
        thirdParty = userRepository.save(thirdParty);

        cameraItem = new Item();
        cameraItem.setOwner(owner);
        cameraItem.setTitle("Sony Alpha A7 III");
        cameraItem.setDescription("Professional full frame camera");
        cameraItem.setCategory(ItemCategory.CAMERAS);
        cameraItem.setPricePerDay(new BigDecimal("100.00"));
        cameraItem.setCondition(ItemCondition.LIKE_NEW);
        cameraItem.setLocation("Block C");
        cameraItem.setAvailable(true);
        cameraItem.addImage("https://res.cloudinary.com/test/sony.jpg");
        cameraItem = itemRepository.save(cameraItem);
    }

    @Test
    void testCreateRentalSuccess() throws Exception {
        String token = jwtService.generateToken(renter.getId());

        LocalDate start = LocalDate.now().plusDays(2);
        LocalDate end = LocalDate.now().plusDays(4); // 3 days inclusive: 2, 3, 4 -> 3 * 100 = 300.00

        CreateRentalRequest request = new CreateRentalRequest(
                cameraItem.getId(),
                start,
                end,
                "Need for weekend photography"
        );

        mockMvc.perform(post("/api/rentals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Rental request submitted"))
                .andExpect(jsonPath("$.data.rental.totalPrice").value(300.00))
                .andExpect(jsonPath("$.data.rental.status").value("pending"))
                .andExpect(jsonPath("$.data.rental.item.title").value("Sony Alpha A7 III"))
                .andExpect(jsonPath("$.data.rental.renter.name").value("Renter Bob"))
                .andExpect(jsonPath("$.data.rental.owner.name").value("Owner Alice"));

        assertThat(notificationRepository.countByRecipientIdAndIsReadFalse(owner.getId())).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testCreateRentalSelfRentForbidden() throws Exception {
        String token = jwtService.generateToken(owner.getId());

        CreateRentalRequest request = new CreateRentalRequest(
                cameraItem.getId(),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                "Renting my own item"
        );

        mockMvc.perform(post("/api/rentals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You cannot rent your own item"));
    }

    @Test
    void testCreateRentalDateOverlapBlocked() throws Exception {
        // Create an approved rental for days +5 to +8
        Rental approvedRental = new Rental();
        approvedRental.setItem(cameraItem);
        approvedRental.setOwner(owner);
        approvedRental.setRenter(thirdParty);
        approvedRental.setStartDate(LocalDate.now().plusDays(5));
        approvedRental.setEndDate(LocalDate.now().plusDays(8));
        approvedRental.setTotalPrice(new BigDecimal("400.00"));
        approvedRental.setStatus(RentalStatus.APPROVED);
        rentalRepository.save(approvedRental);

        // Try booking days +7 to +10 (overlaps on days 7 and 8)
        String token = jwtService.generateToken(renter.getId());

        CreateRentalRequest overlappingRequest = new CreateRentalRequest(
                cameraItem.getId(),
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(10),
                "Overlapping request"
        );

        mockMvc.perform(post("/api/rentals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlappingRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Item is not available for the selected dates"));
    }

    @Test
    void testGetMyRentalsAndReceivedRentals() throws Exception {
        Rental rental = new Rental();
        rental.setItem(cameraItem);
        rental.setOwner(owner);
        rental.setRenter(renter);
        rental.setStartDate(LocalDate.now().plusDays(3));
        rental.setEndDate(LocalDate.now().plusDays(5));
        rental.setTotalPrice(new BigDecimal("300.00"));
        rental.setStatus(RentalStatus.PENDING);
        rentalRepository.save(rental);

        String renterToken = jwtService.generateToken(renter.getId());
        mockMvc.perform(get("/api/rentals/my-rentals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + renterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rentals.length()").value(1));

        String ownerToken = jwtService.generateToken(owner.getId());
        mockMvc.perform(get("/api/rentals/received")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rentals.length()").value(1));
    }

    @Test
    void testGetRentalByIdUnauthorizedUserForbidden() throws Exception {
        Rental rental = new Rental();
        rental.setItem(cameraItem);
        rental.setOwner(owner);
        rental.setRenter(renter);
        rental.setStartDate(LocalDate.now().plusDays(3));
        rental.setEndDate(LocalDate.now().plusDays(5));
        rental.setTotalPrice(new BigDecimal("300.00"));
        rental.setStatus(RentalStatus.PENDING);
        rental = rentalRepository.save(rental);

        String thirdPartyToken = jwtService.generateToken(thirdParty.getId());
        mockMvc.perform(get("/api/rentals/" + rental.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + thirdPartyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testApproveRentalByOwnerAndLifecycle() throws Exception {
        Rental rental = new Rental();
        rental.setItem(cameraItem);
        rental.setOwner(owner);
        rental.setRenter(renter);
        rental.setStartDate(LocalDate.now().plusDays(3));
        rental.setEndDate(LocalDate.now().plusDays(5));
        rental.setTotalPrice(new BigDecimal("300.00"));
        rental.setStatus(RentalStatus.PENDING);
        rental = rentalRepository.save(rental);

        String ownerToken = jwtService.generateToken(owner.getId());
        String renterToken = jwtService.generateToken(renter.getId());

        // 1. Approve (by Owner)
        UpdateRentalStatusRequest approveReq = new UpdateRentalStatusRequest(RentalStatus.APPROVED, "Approved! Pick up at Block C.");
        mockMvc.perform(patch("/api/rentals/" + rental.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rental.status").value("approved"));

        // 2. Active / Handover (by Renter or Owner)
        UpdateRentalStatusRequest activeReq = new UpdateRentalStatusRequest(RentalStatus.ACTIVE, null);
        mockMvc.perform(patch("/api/rentals/" + rental.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + renterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rental.status").value("active"));

        // 3. Complete (by Owner)
        UpdateRentalStatusRequest completeReq = new UpdateRentalStatusRequest(RentalStatus.COMPLETED, "Returned in perfect condition");
        mockMvc.perform(patch("/api/rentals/" + rental.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rental.status").value("completed"));
    }

    @Test
    void testCancelRentalByRenter() throws Exception {
        Rental rental = new Rental();
        rental.setItem(cameraItem);
        rental.setOwner(owner);
        rental.setRenter(renter);
        rental.setStartDate(LocalDate.now().plusDays(3));
        rental.setEndDate(LocalDate.now().plusDays(5));
        rental.setTotalPrice(new BigDecimal("300.00"));
        rental.setStatus(RentalStatus.PENDING);
        rental = rentalRepository.save(rental);

        String renterToken = jwtService.generateToken(renter.getId());

        UpdateRentalStatusRequest cancelReq = new UpdateRentalStatusRequest(RentalStatus.CANCELLED, "Plans changed");
        mockMvc.perform(patch("/api/rentals/" + rental.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + renterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rental.status").value("cancelled"));
    }
}
