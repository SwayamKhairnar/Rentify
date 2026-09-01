package com.rentify.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentify.auth.security.JwtService;
import com.rentify.item.Item;
import com.rentify.item.ItemCategory;
import com.rentify.item.ItemCondition;
import com.rentify.item.ItemRepository;
import com.rentify.rental.Rental;
import com.rentify.rental.RentalRepository;
import com.rentify.rental.RentalStatus;
import com.rentify.report.*;
import com.rentify.report.dto.ResolveReportRequest;
import com.rentify.user.User;
import com.rentify.user.UserRepository;
import com.rentify.user.UserRole;
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
class AdminControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User admin;
    private User student;
    private User badUser;
    private Item item;
    private Rental rental;
    private Report report;

    @BeforeEach
    void setUp() {
        admin = new User("Admin Chief", "admin@example.com", passwordEncoder.encode("password123"), "Admin Campus");
        admin.setRole(UserRole.ADMIN);
        admin = userRepository.save(admin);

        student = new User("Student User", "student@example.com", passwordEncoder.encode("password123"), "North Campus");
        student = userRepository.save(student);

        badUser = new User("Bad Actor", "bad@example.com", passwordEncoder.encode("password123"), "South Campus");
        badUser = userRepository.save(badUser);

        item = new Item();
        item.setOwner(student);
        item.setTitle("DSLR Camera Canon");
        item.setDescription("Professional camera");
        item.setCategory(ItemCategory.CAMERAS);
        item.setPricePerDay(new BigDecimal("120.00"));
        item.setCondition(ItemCondition.LIKE_NEW);
        item.setAvailable(true);
        item = itemRepository.save(item);

        rental = new Rental();
        rental.setItem(item);
        rental.setOwner(student);
        rental.setRenter(badUser);
        rental.setStartDate(LocalDate.now().minusDays(4));
        rental.setEndDate(LocalDate.now().minusDays(1));
        rental.setTotalPrice(new BigDecimal("480.00"));
        rental.setStatus(RentalStatus.COMPLETED);
        rental = rentalRepository.save(rental);

        report = new Report();
        report.setReporter(student);
        report.setReportedUser(badUser);
        report.setRental(rental);
        report.setReason(ReportReason.ITEM_DAMAGE);
        report.setDescription("Damaged camera lens");
        report.setStatus(ReportStatus.PENDING);
        report = reportRepository.save(report);
    }

    @Test
    void testGetStatsAsAdmin() throws Exception {
        String adminToken = jwtService.generateToken(admin.getId());

        mockMvc.perform(get("/api/admin/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stats.totalUsers").isNumber())
                .andExpect(jsonPath("$.data.stats.totalItems").isNumber())
                .andExpect(jsonPath("$.data.stats.pendingReports").value(1));
    }

    @Test
    void testGetStatsAsStudentForbidden() throws Exception {
        String studentToken = jwtService.generateToken(student.getId());

        mockMvc.perform(get("/api/admin/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testToggleUserSuspension() throws Exception {
        String adminToken = jwtService.generateToken(admin.getId());

        // Suspend user
        mockMvc.perform(patch("/api/admin/users/" + badUser.getId() + "/suspend")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.isSuspended").value(true));

        User updatedBadUser = userRepository.findById(badUser.getId()).orElseThrow();
        assertThat(updatedBadUser.isSuspended()).isTrue();

        // Attempt action as suspended user -> 403 Forbidden
        String badUserToken = jwtService.generateToken(badUser.getId());
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + badUserToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Your account has been suspended by an administrator."));
    }

    @Test
    void testResolveReportWithAccountSuspension() throws Exception {
        String adminToken = jwtService.generateToken(admin.getId());

        ResolveReportRequest request = new ResolveReportRequest(
                ReportStatus.RESOLVED,
                AdminAction.ACCOUNT_SUSPENDED,
                "User banned due to severe item destruction."
        );

        mockMvc.perform(patch("/api/admin/reports/" + report.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.report.status").value("resolved"))
                .andExpect(jsonPath("$.data.report.adminAction").value("account_suspended"))
                .andExpect(jsonPath("$.data.report.adminNotes").value("User banned due to severe item destruction."));

        User suspendedUser = userRepository.findById(badUser.getId()).orElseThrow();
        assertThat(suspendedUser.isSuspended()).isTrue();
    }

    @Test
    void testAdminGetUsersAndItemsPaginated() throws Exception {
        String adminToken = jwtService.generateToken(admin.getId());

        // 1. Get users
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pagination.total").isNumber());

        // 2. Get items
        mockMvc.perform(get("/api/admin/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pagination.total").isNumber());

        // 3. Get rentals
        mockMvc.perform(get("/api/admin/rentals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pagination.total").isNumber());
    }
}
