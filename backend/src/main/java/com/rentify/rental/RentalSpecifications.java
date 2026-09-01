package com.rentify.rental;

import org.springframework.data.jpa.domain.Specification;

public class RentalSpecifications {

    public static Specification<Rental> hasStatus(RentalStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}
