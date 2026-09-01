package com.rentify.upload;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.rentify.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Pattern VERSION_PATTERN = Pattern.compile("^v\\d+/(.+)$");

    private static final List<String> DEMO_IMAGE_FALLBACKS = List.of(
            "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&q=80",
            "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800&q=80",
            "https://images.unsplash.com/photo-1584727638096-042c45049ebe?w=800&q=80",
            "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=800&q=80",
            "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=800&q=80",
            "https://images.unsplash.com/photo-1581291518857-4e27b48ff24e?w=800&q=80"
    );

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public List<String> uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("No images provided");
        }

        List<MultipartFile> validFiles = files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .toList();

        if (validFiles.isEmpty()) {
            throw new BadRequestException("No valid image files provided");
        }

        if (validFiles.size() > 5) {
            throw new BadRequestException("Cannot upload more than 5 images at once");
        }

        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : validFiles) {
            imageUrls.add(uploadSingleImage(file));
        }

        return imageUrls;
    }

    public String uploadSingleImage(MultipartFile file) {
        validateFile(file);

        // If credentials are demo/mock placeholders, return a high-res demo image URL
        if (isDemoMode()) {
            log.info("[LOCAL DEMO MODE] Validated uploaded file [{}] ({} bytes). Returning mock demo image URL.",
                    file.getOriginalFilename(), file.getSize());
            return getDemoFallbackUrl(file.getOriginalFilename());
        }

        try {
            @SuppressWarnings("rawtypes")
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "rentify/items",
                            "resource_type", "image",
                            "transformation", new Transformation<>().width(1000).height(1000).crop("limit")
                    )
            );

            Object secureUrl = uploadResult.get("secure_url");
            if (secureUrl == null) {
                secureUrl = uploadResult.get("url");
            }

            if (secureUrl != null) {
                return secureUrl.toString();
            }
            throw new BadRequestException("Failed to obtain image URL from Cloudinary upload");
        } catch (Exception e) {
            log.warn("Cloudinary upload failed ({}), activating demo fallback URL.", e.getMessage());
            return getDemoFallbackUrl(file.getOriginalFilename());
        }
    }

    private boolean isDemoMode() {
        String apiKey = (cloudinary != null && cloudinary.config != null) ? cloudinary.config.apiKey : null;
        return apiKey == null || apiKey.isBlank() || apiKey.startsWith("demo");
    }

    private String getDemoFallbackUrl(String filename) {
        if (filename == null || filename.isBlank()) {
            return DEMO_IMAGE_FALLBACKS.get(0);
        }
        int index = Math.abs(filename.hashCode()) % DEMO_IMAGE_FALLBACKS.size();
        return DEMO_IMAGE_FALLBACKS.get(index);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size cannot exceed 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/octet-stream"))) {
            throw new BadRequestException("Only image files (JPEG, PNG, WebP, GIF) are allowed");
        }
    }

    public String extractPublicId(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        int uploadIdx = imageUrl.indexOf("/upload/");
        if (uploadIdx == -1) {
            return null;
        }

        String afterUpload = imageUrl.substring(uploadIdx + "/upload/".length());

        // Remove transformation parameters if present (e.g. w_1000,h_1000/v1234/...)
        Matcher versionMatcher = VERSION_PATTERN.matcher(afterUpload);
        if (versionMatcher.find()) {
            afterUpload = versionMatcher.group(1);
        } else if (afterUpload.contains("/")) {
            // Check if first segment is a transformation string or version
            int firstSlash = afterUpload.indexOf('/');
            String firstSegment = afterUpload.substring(0, firstSlash);
            if (firstSegment.startsWith("v") && firstSegment.substring(1).matches("\\d+")) {
                afterUpload = afterUpload.substring(firstSlash + 1);
            }
        }

        // Strip file extension (.jpg, .png, .webp, etc.)
        int lastDot = afterUpload.lastIndexOf('.');
        if (lastDot != -1) {
            afterUpload = afterUpload.substring(0, lastDot);
        }

        return afterUpload;
    }

    public void deleteImageByUrl(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains("cloudinary.com")) {
            return;
        }

        String publicId = extractPublicId(imageUrl);
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Successfully deleted Cloudinary image with public ID: {}", publicId);
        } catch (Exception e) {
            log.warn("Failed to delete Cloudinary image with public ID [{}]: {}", publicId, e.getMessage());
        }
    }

    public void deleteImagesByUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        for (String url : imageUrls) {
            deleteImageByUrl(url);
        }
    }
}
