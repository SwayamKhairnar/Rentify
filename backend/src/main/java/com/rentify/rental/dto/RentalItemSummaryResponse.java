package com.rentify.rental.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentify.item.Item;
import com.rentify.item.ItemCategory;
import com.rentify.item.ItemImage;

import java.math.BigDecimal;
import java.util.List;

public record RentalItemSummaryResponse(
    Long id,
    String title,
    ItemCategory category,
    BigDecimal pricePerDay,
    String location,
    List<String> images
) {
    @JsonProperty("_id")
    public Long getMongoId() {
        return id;
    }

    public static RentalItemSummaryResponse fromEntity(Item item) {
        if (item == null) return null;
        List<String> imageUrls = item.getImages() != null
                ? item.getImages().stream().map(ItemImage::getImageUrl).toList()
                : List.of();
        return new RentalItemSummaryResponse(
            item.getId(),
            item.getTitle(),
            item.getCategory(),
            item.getPricePerDay(),
            item.getLocation(),
            imageUrls
        );
    }
}
