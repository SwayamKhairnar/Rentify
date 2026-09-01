package com.rentify.upload;

import com.rentify.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final CloudinaryService cloudinaryService;

    public UploadController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> uploadImages(
            @RequestParam("images") List<MultipartFile> files
    ) {
        List<String> imageUrls = cloudinaryService.uploadImages(files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Images uploaded successfully", Map.of("imageUrls", imageUrls)));
    }
}
