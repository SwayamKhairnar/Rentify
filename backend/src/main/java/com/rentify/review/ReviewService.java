package com.rentify.review;

import com.rentify.common.PaginatedResponse;
import com.rentify.exception.BadRequestException;
import com.rentify.exception.ConflictException;
import com.rentify.exception.ForbiddenException;
import com.rentify.exception.NotFoundException;
import com.rentify.notification.NotificationService;
import com.rentify.notification.NotificationType;
import com.rentify.rental.Rental;
import com.rentify.rental.RentalRepository;
import com.rentify.rental.RentalStatus;
import com.rentify.review.dto.CreateReviewRequest;
import com.rentify.review.dto.ReviewResponse;
import com.rentify.user.User;
import com.rentify.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final RatingAggregationService ratingAggregationService;
    private final NotificationService notificationService;

    public ReviewService(
            ReviewRepository reviewRepository,
            RentalRepository rentalRepository,
            UserRepository userRepository,
            RatingAggregationService ratingAggregationService,
            NotificationService notificationService
    ) {
        this.reviewRepository = reviewRepository;
        this.rentalRepository = rentalRepository;
        this.userRepository = userRepository;
        this.ratingAggregationService = ratingAggregationService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReviewResponse createReview(Long reviewerId, CreateReviewRequest request) {
        Rental rental = rentalRepository.findById(request.rentalId())
                .orElseThrow(() -> new NotFoundException("Rental not found"));

        if (rental.getStatus() != RentalStatus.COMPLETED) {
            throw new BadRequestException("Can only review completed rentals");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        boolean isRenter = rental.getRenter().getId().equals(reviewerId);
        boolean isOwner = rental.getOwner().getId().equals(reviewerId);

        if (!isRenter && !isOwner) {
            throw new ForbiddenException("You are not authorized to review this rental");
        }

        if (reviewRepository.existsByRentalIdAndReviewerId(rental.getId(), reviewerId)) {
            throw new ConflictException("You have already reviewed this rental");
        }

        User targetUser = isRenter ? rental.getOwner() : rental.getRenter();
        ReviewType type = isRenter ? ReviewType.LENDER : ReviewType.RENTER;
        Integer itemRating = isRenter ? request.itemRating() : null;

        Review review = new Review();
        review.setRental(rental);
        review.setReviewer(reviewer);
        review.setReviewee(targetUser);
        review.setType(type);
        review.setRating(request.rating());
        review.setItemRating(itemRating);
        review.setComment(request.comment() != null ? request.comment().trim() : "");

        Review savedReview = reviewRepository.save(review);

        ratingAggregationService.recalculateUserRatings(targetUser.getId());
        if (isRenter && rental.getItem() != null) {
            ratingAggregationService.recalculateItemRatings(rental.getItem().getId());
        }

        notificationService.createNotification(
                targetUser,
                reviewer,
                NotificationType.REVIEW_RECEIVED,
                "New Review Received",
                reviewer.getName() + " left you a " + request.rating() + "-star review.",
                "/users/" + targetUser.getId()
        );

        return ReviewResponse.fromEntity(savedReview);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ReviewResponse> getReviewsByUser(Long userId, int page, int limit) {
        int validatedPage = Math.max(page, 1);
        int validatedLimit = Math.min(Math.max(limit, 1), 50);

        Pageable pageable = PageRequest.of(validatedPage - 1, validatedLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> pageResult = reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId, pageable);

        List<ReviewResponse> responses = pageResult.getContent().stream()
                .map(ReviewResponse::fromEntity)
                .toList();

        return PaginatedResponse.of("User reviews fetched", responses, validatedPage, validatedLimit, pageResult.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByRental(Long rentalId) {
        List<Review> reviews = reviewRepository.findByRentalIdOrderByCreatedAtDesc(rentalId);
        return reviews.stream()
                .map(ReviewResponse::fromEntity)
                .toList();
    }
}
