package com.rentify.upload;

import com.rentify.auth.security.JwtService;
import com.rentify.user.User;
import com.rentify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UploadControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private CloudinaryService cloudinaryService;

    private User student;

    @BeforeEach
    void setUp() {
        student = new User("Upload Student", "uploader@example.com", passwordEncoder.encode("password123"), "North Campus");
        student = userRepository.save(student);
    }

    @Test
    void testUploadImagesSuccess() throws Exception {
        String token = jwtService.generateToken(student.getId());

        MockMultipartFile file1 = new MockMultipartFile(
                "images", "photo1.jpg", "image/jpeg", "fake image 1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "images", "photo2.png", "image/png", "fake image 2".getBytes()
        );

        Mockito.when(cloudinaryService.uploadImages(anyList()))
                .thenReturn(List.of(
                        "https://res.cloudinary.com/test/photo1.jpg",
                        "https://res.cloudinary.com/test/photo2.png"
                ));

        mockMvc.perform(multipart("/api/upload")
                        .file(file1)
                        .file(file2)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Images uploaded successfully"))
                .andExpect(jsonPath("$.data.imageUrls").isArray())
                .andExpect(jsonPath("$.data.imageUrls.length()").value(2))
                .andExpect(jsonPath("$.data.imageUrls[0]").value("https://res.cloudinary.com/test/photo1.jpg"))
                .andExpect(jsonPath("$.data.imageUrls[1]").value("https://res.cloudinary.com/test/photo2.png"));
    }

    @Test
    void testUploadImagesUnauthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "images", "photo1.jpg", "image/jpeg", "fake image".getBytes()
        );

        mockMvc.perform(multipart("/api/upload")
                        .file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
