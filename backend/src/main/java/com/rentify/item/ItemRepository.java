package com.rentify.item;

import com.rentify.rental.RentalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {
    List<Item> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    long countByIsAvailableTrue();

    @Query("""
        select count(r) > 0
        from Rental r
        where r.item.id = :itemId
          and r.status in :statuses
    """)
    boolean hasRentalsWithStatuses(@Param("itemId") Long itemId, @Param("statuses") Collection<RentalStatus> statuses);

    @Query("""
        select i from Item i
        where (:search is null or lower(i.title) like lower(concat('%', :search, '%')) or lower(i.description) like lower(concat('%', :search, '%')))
          and (:category is null or i.category = :category)
          and (:available is null or i.isAvailable = :available)
        order by i.createdAt desc
    """)
    Page<Item> findFilteredItems(
        @Param("search") String search,
        @Param("category") ItemCategory category,
        @Param("available") Boolean available,
        Pageable pageable
    );
}
