package com.rentify.review;

import com.rentify.auth.security.CustomUserDetails;
import com.rentify.common.ApiResponse;
import com.rentify.common.CurrentUser;
import com.rentify.common.PaginatedResponse;
import com.rentify.review.dto.CreateReviewRequest;
import com.rentify.review.dto.ReviewResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, ReviewResponse>>> createReview(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        ReviewResponse review = reviewService.createReview(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Review submitted successfully", Map.of("review", review)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PaginatedResponse<ReviewResponse>> getReviewsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        PaginatedResponse<ReviewResponse> response = reviewService.getReviewsByUser(userId, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rental/{rentalId}")
    public ResponseEntity<ApiResponse<Map<String, List<ReviewResponse>>>> getReviewsByRental(
            @PathVariable Long rentalId
    ) {
        List<ReviewResponse> reviews = reviewService.getReviewsByRental(rentalId);
        return ResponseEntity.ok(ApiResponse.success("Rental reviews fetched", Map.of("reviews", reviews)));
    }
}
