package com.rentify.rental;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long>, JpaSpecificationExecutor<Rental> {

    @Query("""
        select r
        from Rental r
        where r.item.id = :itemId
          and r.status in :statuses
          and :startDate <= r.endDate
          and :endDate >= r.startDate
    """)
    List<Rental> findOverlappingRentals(
        @Param("itemId") Long itemId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuses") Collection<RentalStatus> statuses
    );

    default boolean hasOverlappingRentals(Long itemId, LocalDate startDate, LocalDate endDate, Collection<RentalStatus> statuses) {
        return !findOverlappingRentals(itemId, startDate, endDate, statuses).isEmpty();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select r
        from Rental r
        where r.item.id = :itemId
          and r.status in :statuses
          and :startDate <= r.endDate
          and :endDate >= r.startDate
    """)
    List<Rental> findOverlappingRentalsForUpdate(
        @Param("itemId") Long itemId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuses") Collection<RentalStatus> statuses
    );

    List<Rental> findByRenterIdOrderByCreatedAtDesc(Long renterId);

    List<Rental> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    long countByStatus(RentalStatus status);

    @Query("""
        select r
        from Rental r
        where r.item.id = :itemId
          and r.status = com.rentify.rental.RentalStatus.PENDING
          and r.id <> :approvedRentalId
          and :startDate <= r.endDate
          and :endDate >= r.startDate
    """)
    List<Rental> findConflictingPendingRentals(
        @Param("itemId") Long itemId,
        @Param("approvedRentalId") Long approvedRentalId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    List<Rental> findByItemId(Long itemId);
}
