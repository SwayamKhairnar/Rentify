package com.rentify.report;

import org.springframework.data.jpa.domain.Specification;

public class ReportSpecifications {

    public static Specification<Report> hasStatus(ReportStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Report> hasReason(ReportReason reason) {
        return (root, query, cb) -> reason == null ? null : cb.equal(root.get("reason"), reason);
    }
}
