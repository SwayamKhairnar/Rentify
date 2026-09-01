package com.rentify.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByRentalIdAndReviewerId(Long rentalId, Long reviewerId);

    List<Review> findByRevieweeId(Long revieweeId);

    Page<Review> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId, Pageable pageable);

    List<Review> findByRentalIdOrderByCreatedAtDesc(Long rentalId);

    @Query("select r from Review r where r.rental.item.id = :itemId and r.itemRating is not null")
    List<Review> findByItemIdWithItemRating(@Param("itemId") Long itemId);
}
