package com.rentify.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    List<Report> findAllByOrderByCreatedAtDesc();

    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    long countByStatus(ReportStatus status);

    @Query("""
        select r
        from Report r
        where r.reporter.id = :userId or r.reportedUser.id = :userId
        order by r.createdAt desc
    """)
    List<Report> findByUserInvolvement(@Param("userId") Long userId);

    List<Report> findByRentalId(Long rentalId);
}
