package com.rentify.item.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentify.item.Item;
import com.rentify.item.ItemCategory;
import com.rentify.item.ItemCondition;
import com.rentify.item.ItemImage;
import com.rentify.user.dto.OwnerSummaryResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ItemResponse(
    Long id,
    String title,
    String description,
    ItemCategory category,
    BigDecimal pricePerDay,
    ItemCondition condition,
    boolean isAvailable,
    String location,
    BigDecimal rating,
    Integer totalReviews,
    List<String> images,
    OwnerSummaryResponse owner,
    Instant createdAt,
    Instant updatedAt
) {
    @JsonProperty("_id")
    public Long getMongoId() {
        return id;
    }

    public static ItemResponse fromEntity(Item item) {
        if (item == null) return null;
        List<String> imageUrls = item.getImages() != null
                ? item.getImages().stream().map(ItemImage::getImageUrl).toList()
                : List.of();

        return new ItemResponse(
            item.getId(),
            item.getTitle(),
            item.getDescription(),
            item.getCategory(),
            item.getPricePerDay(),
            item.getCondition(),
            item.isAvailable(),
            item.getLocation(),
            item.getRating(),
            item.getTotalReviews(),
            imageUrls,
            OwnerSummaryResponse.fromEntity(item.getOwner()),
            item.getCreatedAt(),
            item.getUpdatedAt()
        );
    }
}
