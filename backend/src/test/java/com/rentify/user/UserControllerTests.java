package com.rentify.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentify.auth.security.JwtService;
import com.rentify.user.dto.UpdateProfileRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User student;

    @BeforeEach
    void setUp() {
        student = new User("John Doe", "john@example.com", passwordEncoder.encode("password123"), "North Campus");
        student.setBio("Computer Science student");
        student = userRepository.save(student);
    }

    @Test
    void testGetPublicProfileSuccess() throws Exception {
        mockMvc.perform(get("/api/users/" + student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User fetched"))
                .andExpect(jsonPath("$.data.user.name").value("John Doe"))
                .andExpect(jsonPath("$.data.user.campus").value("North Campus"))
                .andExpect(jsonPath("$.data.user.bio").value("Computer Science student"))
                .andExpect(jsonPath("$.data.user._id").value(student.getId()));
    }

    @Test
    void testGetPublicProfileNotFound() throws Exception {
        mockMvc.perform(get("/api/users/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void testUpdateProfileAuthenticated() throws Exception {
        String token = jwtService.generateToken(student.getId());

        UpdateProfileRequest request = new UpdateProfileRequest(
                "John Updated",
                "Updated bio text",
                "South Campus",
                "+91 9876543210",
                "https://res.cloudinary.com/test/avatar.jpg"
        );

        mockMvc.perform(put("/api/users/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile updated"))
                .andExpect(jsonPath("$.data.user.name").value("John Updated"))
                .andExpect(jsonPath("$.data.user.campus").value("South Campus"))
                .andExpect(jsonPath("$.data.user.bio").value("Updated bio text"))
                .andExpect(jsonPath("$.data.user.phone").value("+91 9876543210"))
                .andExpect(jsonPath("$.data.user.avatar").value("https://res.cloudinary.com/test/avatar.jpg"));
    }

    @Test
    void testUpdateProfileUnauthenticated() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("New Name", null, null, null, null);

        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
