package com.rentify.review;

import com.rentify.item.Item;
import com.rentify.item.ItemRepository;
import com.rentify.user.User;
import com.rentify.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class RatingAggregationService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    public RatingAggregationService(
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            ItemRepository itemRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public void recalculateUserRatings(Long userId) {
        if (userId == null) return;

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        List<Review> reviews = reviewRepository.findByRevieweeId(userId);

        List<Review> lenderReviews = reviews.stream()
                .filter(r -> r.getType() == ReviewType.LENDER)
                .toList();
        int totalLenderReviews = lenderReviews.size();
        BigDecimal lenderRating = totalLenderReviews > 0
                ? round(lenderReviews.stream().mapToInt(Review::getRating).average().orElse(0.0))
                : BigDecimal.ZERO;

        List<Review> renterReviews = reviews.stream()
                .filter(r -> r.getType() == ReviewType.RENTER)
                .toList();
        int totalRenterReviews = renterReviews.size();
        BigDecimal renterRating = totalRenterReviews > 0
                ? round(renterReviews.stream().mapToInt(Review::getRating).average().orElse(0.0))
                : BigDecimal.ZERO;

        int totalReviews = reviews.size();
        BigDecimal overallRating = totalReviews > 0
                ? round(reviews.stream().mapToInt(Review::getRating).average().orElse(0.0))
                : BigDecimal.ZERO;

        List<Review> itemQualityReviews = reviews.stream()
                .filter(r -> r.getItemRating() != null)
                .toList();
        int totalItemQualityReviews = itemQualityReviews.size();
        BigDecimal itemQualityAverage = totalItemQualityReviews > 0
                ? round(itemQualityReviews.stream().mapToInt(Review::getItemRating).average().orElse(0.0))
                : BigDecimal.ZERO;

        user.setLenderRating(lenderRating);
        user.setTotalLenderReviews(totalLenderReviews);
        user.setRenterRating(renterRating);
        user.setTotalRenterReviews(totalRenterReviews);
        user.setRating(overallRating);
        user.setTotalReviews(totalReviews);
        user.setItemQualityAverage(itemQualityAverage);
        user.setTotalItemQualityReviews(totalItemQualityReviews);

        userRepository.save(user);
    }

    @Transactional
    public void recalculateItemRatings(Long itemId) {
        if (itemId == null) return;

        Item item = itemRepository.findById(itemId).orElse(null);
        if (item == null) return;

        List<Review> itemReviews = reviewRepository.findByItemIdWithItemRating(itemId);
        int totalReviews = itemReviews.size();
        BigDecimal rating = totalReviews > 0
                ? round(itemReviews.stream().mapToInt(Review::getItemRating).average().orElse(0.0))
                : BigDecimal.ZERO;

        item.setRating(rating);
        item.setTotalReviews(totalReviews);

        itemRepository.save(item);
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }
}
