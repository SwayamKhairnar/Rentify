package com.rentify.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentify.auth.security.JwtService;
import com.rentify.item.Item;
import com.rentify.item.ItemCategory;
import com.rentify.item.ItemCondition;
import com.rentify.item.ItemRepository;
import com.rentify.rental.Rental;
import com.rentify.rental.RentalRepository;
import com.rentify.rental.RentalStatus;
import com.rentify.report.dto.CreateReportRequest;
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
class ReportControllerTests {

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

    private User reporter;
    private User reportedUser;
    private Rental rental;

    @BeforeEach
    void setUp() {
        reporter = new User("Alice Reporter", "alice@example.com", passwordEncoder.encode("password123"), "North Campus");
        reporter = userRepository.save(reporter);

        reportedUser = new User("Bob Scammer", "bob@example.com", passwordEncoder.encode("password123"), "South Campus");
        reportedUser = userRepository.save(reportedUser);

        Item item = new Item();
        item.setOwner(reportedUser);
        item.setTitle("Broken Drone");
        item.setDescription("Drone that does not fly");
        item.setCategory(ItemCategory.ELECTRONICS);
        item.setPricePerDay(new BigDecimal("100.00"));
        item.setCondition(ItemCondition.GOOD);
        item = itemRepository.save(item);

        rental = new Rental();
        rental.setItem(item);
        rental.setOwner(reportedUser);
        rental.setRenter(reporter);
        rental.setStartDate(LocalDate.now().minusDays(3));
        rental.setEndDate(LocalDate.now().minusDays(1));
        rental.setTotalPrice(new BigDecimal("300.00"));
        rental.setStatus(RentalStatus.COMPLETED);
        rental = rentalRepository.save(rental);
    }

    @Test
    void testCreateReportSuccess() throws Exception {
        String token = jwtService.generateToken(reporter.getId());

        CreateReportRequest request = new CreateReportRequest(
                reportedUser.getId(),
                rental.getId(),
                ReportReason.ITEM_DAMAGE,
                "The drone returned with a cracked propeller",
                "https://cloudinary.com/evidence.jpg"
        );

        mockMvc.perform(post("/api/reports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Report submitted successfully"))
                .andExpect(jsonPath("$.data.report.reason").value("Item Damage"))
                .andExpect(jsonPath("$.data.report.reporter.name").value("Alice Reporter"))
                .andExpect(jsonPath("$.data.report.reportedUser.name").value("Bob Scammer"))
                .andExpect(jsonPath("$.data.report.status").value("pending"));

        assertThat(reportRepository.findByReporterIdOrderByCreatedAtDesc(reporter.getId())).hasSize(1);
    }

    @Test
    void testCreateReportSelfReportRejected() throws Exception {
        String token = jwtService.generateToken(reporter.getId());

        CreateReportRequest request = new CreateReportRequest(
                reporter.getId(),
                rental.getId(),
                ReportReason.OTHER,
                "Reporting myself by mistake",
                null
        );

        mockMvc.perform(post("/api/reports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You cannot report yourself"));
    }

    @Test
    void testGetMyReports() throws Exception {
        String token = jwtService.generateToken(reporter.getId());

        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedUser(reportedUser);
        report.setRental(rental);
        report.setReason(ReportReason.NO_SHOW);
        report.setDescription("User did not show up");
        report.setStatus(ReportStatus.PENDING);
        reportRepository.save(report);

        mockMvc.perform(get("/api/reports/my-reports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reports.length()").value(1))
                .andExpect(jsonPath("$.data.reports[0].reason").value("No Show"));
    }
}
