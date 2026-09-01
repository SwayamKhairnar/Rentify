package com.rentify.user;

import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {

    public static Specification<User> hasRole(UserRole role) {
        return (root, query, cb) -> role == null ? null : cb.equal(root.get("role"), role);
    }

    public static Specification<User> isSuspended(Boolean suspended) {
        return (root, query, cb) -> suspended == null ? null : cb.equal(root.get("isSuspended"), suspended);
    }

    public static Specification<User> search(String queryStr) {
        return (root, query, cb) -> {
            if (queryStr == null || queryStr.isBlank()) {
                return null;
            }
            String pattern = "%" + queryStr.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
            );
        };
    }
}
