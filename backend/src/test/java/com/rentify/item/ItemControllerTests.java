package com.rentify.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentify.auth.security.JwtService;
import com.rentify.item.dto.CreateItemRequest;
import com.rentify.item.dto.UpdateItemRequest;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ItemControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User owner;
    private User otherStudent;
    private Item cameraItem;
    private Item bikeItem;

    @BeforeEach
    void setUp() {
        owner = new User("Owner Alice", "alice@example.com", passwordEncoder.encode("password123"), "North Campus");
        owner = userRepository.save(owner);

        otherStudent = new User("Bob Student", "bob@example.com", passwordEncoder.encode("password123"), "South Campus");
        otherStudent = userRepository.save(otherStudent);

        cameraItem = new Item();
        cameraItem.setOwner(owner);
        cameraItem.setTitle("Sony Alpha A6400 Mirrorless Camera");
        cameraItem.setDescription("High quality 4K video and photography camera");
        cameraItem.setCategory(ItemCategory.CAMERAS);
        cameraItem.setPricePerDay(new BigDecimal("500.00"));
        cameraItem.setCondition(ItemCondition.LIKE_NEW);
        cameraItem.setLocation("Block B");
        cameraItem.addImage("https://res.cloudinary.com/test/sony1.jpg");
        cameraItem = itemRepository.save(cameraItem);

        bikeItem = new Item();
        bikeItem.setOwner(owner);
        bikeItem.setTitle("Hero Sprint Geared Bicycle");
        bikeItem.setDescription("Fast hybrid bike for campus commutes");
        bikeItem.setCategory(ItemCategory.BIKES);
        bikeItem.setPricePerDay(new BigDecimal("100.00"));
        bikeItem.setCondition(ItemCondition.GOOD);
        bikeItem.setLocation("Block A");
        bikeItem = itemRepository.save(bikeItem);
    }

    @Test
    void testGetItemsCatalog() throws Exception {
        mockMvc.perform(get("/api/items?page=1&limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Items fetched"))
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.limit").value(10))
                .andExpect(jsonPath("$.pagination.total").value(2))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void testGetItemsFilterByCategoryAndSearch() throws Exception {
        mockMvc.perform(get("/api/items?category=cameras&search=sony"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pagination.total").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Sony Alpha A6400 Mirrorless Camera"))
                .andExpect(jsonPath("$.data[0].category").value("cameras"));
    }

    @Test
    void testGetItemByIdSuccess() throws Exception {
        mockMvc.perform(get("/api/items/" + cameraItem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.item.title").value("Sony Alpha A6400 Mirrorless Camera"))
                .andExpect(jsonPath("$.data.item.owner.name").value("Owner Alice"))
                .andExpect(jsonPath("$.data.item.owner.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.item.images[0]").value("https://res.cloudinary.com/test/sony1.jpg"))
                .andExpect(jsonPath("$.data.item._id").value(cameraItem.getId()));
    }

    @Test
    void testGetItemByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/items/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Item not found"));
    }

    @Test
    void testGetMyItemsAuthenticated() throws Exception {
        String token = jwtService.generateToken(owner.getId());

        mockMvc.perform(get("/api/items/mine")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Your items fetched"))
                .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    void testCreateItemSuccess() throws Exception {
        String token = jwtService.generateToken(owner.getId());

        CreateItemRequest request = new CreateItemRequest(
                "Calculus Early Transcendentals",
                "Textbook for first year math",
                ItemCategory.TEXTBOOKS,
                new BigDecimal("40.00"),
                List.of("https://res.cloudinary.com/test/book.jpg"),
                ItemCondition.LIKE_NEW,
                "Library Block"
        );

        mockMvc.perform(post("/api/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item created"))
                .andExpect(jsonPath("$.data.item.title").value("Calculus Early Transcendentals"))
                .andExpect(jsonPath("$.data.item.category").value("textbooks"))
                .andExpect(jsonPath("$.data.item.images.length()").value(1));
    }

    @Test
    void testUpdateItemByOwnerSuccess() throws Exception {
        String token = jwtService.generateToken(owner.getId());

        UpdateItemRequest request = new UpdateItemRequest(
                "Sony Alpha A6400 (Updated)",
                null,
                null,
                new BigDecimal("550.00"),
                null,
                null,
                null,
                null
        );

        mockMvc.perform(put("/api/items/" + cameraItem.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item updated"))
                .andExpect(jsonPath("$.data.item.title").value("Sony Alpha A6400 (Updated)"))
                .andExpect(jsonPath("$.data.item.pricePerDay").value(550.00));
    }

    @Test
    void testUpdateItemByNonOwnerForbidden() throws Exception {
        String token = jwtService.generateToken(otherStudent.getId());

        UpdateItemRequest request = new UpdateItemRequest(
                "Hacked title", null, null, null, null, null, null, null
        );

        mockMvc.perform(put("/api/items/" + cameraItem.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You are not authorized to update this item"));
    }

    @Test
    void testDeleteItemSuccessAndAutoCancelPendingRentals() throws Exception {
        // Create pending rental on bikeItem
        Rental pendingRental = new Rental();
        pendingRental.setItem(bikeItem);
        pendingRental.setRenter(otherStudent);
        pendingRental.setOwner(owner);
        pendingRental.setStartDate(LocalDate.of(2026, 9, 10));
        pendingRental.setEndDate(LocalDate.of(2026, 9, 12));
        pendingRental.setTotalPrice(new BigDecimal("200.00"));
        pendingRental.setStatus(RentalStatus.PENDING);
        pendingRental = rentalRepository.save(pendingRental);

        String token = jwtService.generateToken(owner.getId());

        mockMvc.perform(delete("/api/items/" + bikeItem.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item deleted"));

        assertThat(itemRepository.findById(bikeItem.getId())).isEmpty();

        Rental updatedRental = rentalRepository.findById(pendingRental.getId()).orElseThrow();
        assertThat(updatedRental.getStatus()).isEqualTo(RentalStatus.CANCELLED);
        assertThat(updatedRental.getMessage()).isEqualTo("The item has been deleted by the owner.");
    }

    @Test
    void testDeleteItemWithActiveRentalsRejected() throws Exception {
        // Create approved/active rental on cameraItem
        Rental activeRental = new Rental();
        activeRental.setItem(cameraItem);
        activeRental.setRenter(otherStudent);
        activeRental.setOwner(owner);
        activeRental.setStartDate(LocalDate.of(2026, 9, 10));
        activeRental.setEndDate(LocalDate.of(2026, 9, 12));
        activeRental.setTotalPrice(new BigDecimal("1000.00"));
        activeRental.setStatus(RentalStatus.APPROVED);
        rentalRepository.save(activeRental);

        String token = jwtService.generateToken(owner.getId());

        mockMvc.perform(delete("/api/items/" + cameraItem.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot delete item with active or approved rentals"));

        assertThat(itemRepository.findById(cameraItem.getId())).isPresent();
    }
}
