package com.rentify.item;

import org.springframework.data.jpa.domain.Specification;

public class ItemSpecifications {

    public static Specification<Item> isAvailable(Boolean isAvailable) {
        return (root, query, cb) -> isAvailable == null ? null : cb.equal(root.get("isAvailable"), isAvailable);
    }

    public static Specification<Item> hasCategory(ItemCategory category) {
        return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<Item> hasCondition(ItemCondition condition) {
        return (root, query, cb) -> condition == null ? null : cb.equal(root.get("condition"), condition);
    }

    public static Specification<Item> hasOwner(Long ownerId) {
        return (root, query, cb) -> ownerId == null ? null : cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<Item> search(String queryStr) {
        return (root, query, cb) -> {
            if (queryStr == null || queryStr.isBlank()) {
                return null;
            }
            String pattern = "%" + queryStr.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }
}
