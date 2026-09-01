package com.rentify.upload;

import com.cloudinary.Cloudinary;
import com.rentify.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CloudinaryServiceTests {

    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        Cloudinary cloudinary = new Cloudinary(Map.of("cloud_name", "test", "api_key", "123", "api_secret", "abc"));
        cloudinaryService = new CloudinaryService(cloudinary);
    }

    @Test
    void testExtractPublicIdStandardUrl() {
        String url = "https://res.cloudinary.com/demo/image/upload/v1570979139/rentify/items/sample_camera.jpg";
        String publicId = cloudinaryService.extractPublicId(url);
        assertThat(publicId).isEqualTo("rentify/items/sample_camera");
    }

    @Test
    void testExtractPublicIdWithoutVersion() {
        String url = "https://res.cloudinary.com/demo/image/upload/rentify/items/sample_bike.png";
        String publicId = cloudinaryService.extractPublicId(url);
        assertThat(publicId).isEqualTo("rentify/items/sample_bike");
    }

    @Test
    void testExtractPublicIdInvalidUrl() {
        assertThat(cloudinaryService.extractPublicId(null)).isNull();
        assertThat(cloudinaryService.extractPublicId("https://example.com/other.jpg")).isNull();
    }

    @Test
    void testValidateFileTooLarge() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile("images", "large.jpg", "image/jpeg", largeBytes);

        assertThatThrownBy(() -> cloudinaryService.uploadSingleImage(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File size cannot exceed 5MB");
    }

    @Test
    void testValidateInvalidMimeType() {
        byte[] bytes = "document content".getBytes();
        MockMultipartFile file = new MockMultipartFile("images", "doc.pdf", "application/pdf", bytes);

        assertThatThrownBy(() -> cloudinaryService.uploadSingleImage(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only image files");
    }

    @Test
    void testValidateTooManyFiles() {
        MockMultipartFile file = new MockMultipartFile("images", "img.jpg", "image/jpeg", "content".getBytes());
        List<MockMultipartFile> files = List.of(file, file, file, file, file, file); // 6 files

        assertThatThrownBy(() -> cloudinaryService.uploadImages(List.copyOf(files)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot upload more than 5 images");
    }
}
