package com.rentify.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentify.auth.dto.LoginRequest;
import com.rentify.auth.dto.RegisterRequest;
import com.rentify.auth.security.JwtService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTests {

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

    private User activeUser;
    private User suspendedUser;

    @BeforeEach
    void setUp() {
        activeUser = new User("Active Student", "active@example.com", passwordEncoder.encode("password123"), "North Campus");
        activeUser = userRepository.save(activeUser);

        suspendedUser = new User("Suspended Student", "suspended@example.com", passwordEncoder.encode("password123"), "South Campus");
        suspendedUser.setSuspended(true);
        suspendedUser = userRepository.save(suspendedUser);
    }

    @Test
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest("New Student", "newstudent@example.com", "secret123", "Main Campus");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.data.user.name").value("New Student"))
                .andExpect(jsonPath("$.data.user.email").value("newstudent@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("student"))
                .andExpect(jsonPath("$.data.user._id").isNumber())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("Active Duplicate", "ACTIVE@example.com", "secret123", "Main Campus");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void testRegisterValidationFailure() throws Exception {
        RegisterRequest request = new RegisterRequest("", "invalid-email", "123", "");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testLoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("active@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.user.name").value("Active Student"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void testLoginBadCredentials() throws Exception {
        LoginRequest wrongPassword = new LoginRequest("active@example.com", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPassword)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        LoginRequest unknownUser = new LoginRequest("unknown@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownUser)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testLoginSuspendedUserBlocked() throws Exception {
        LoginRequest request = new LoginRequest("suspended@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Your account has been suspended by an administrator."));
    }

    @Test
    void testGetMeAuthenticated() throws Exception {
        String token = jwtService.generateToken(activeUser.getId());

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile fetched"))
                .andExpect(jsonPath("$.data.user.name").value("Active Student"))
                .andExpect(jsonPath("$.data.user.email").value("active@example.com"))
                .andExpect(jsonPath("$.data.user._id").value(activeUser.getId()));
    }

    @Test
    void testGetMeUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access token is required"));
    }

    @Test
    void testSuspendedUserTokenBlockedOnAuthenticatedEndpoint() throws Exception {
        String token = jwtService.generateToken(suspendedUser.getId());

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Your account has been suspended by an administrator."));
    }
}
