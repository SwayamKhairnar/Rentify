package com.rentify.admin;

import com.rentify.admin.dto.AdminStatsResponse;
import com.rentify.common.PaginatedResponse;
import com.rentify.exception.BadRequestException;
import com.rentify.exception.NotFoundException;
import com.rentify.item.Item;
import com.rentify.item.ItemCategory;
import com.rentify.item.ItemRepository;
import com.rentify.item.ItemSpecifications;
import com.rentify.item.dto.ItemResponse;
import com.rentify.rental.Rental;
import com.rentify.rental.RentalRepository;
import com.rentify.rental.RentalSpecifications;
import com.rentify.rental.RentalStatus;
import com.rentify.rental.dto.RentalResponse;
import com.rentify.report.ReportRepository;
import com.rentify.report.ReportStatus;
import com.rentify.review.ReviewRepository;
import com.rentify.user.User;
import com.rentify.user.UserRepository;
import com.rentify.user.UserRole;
import com.rentify.user.UserSpecifications;
import com.rentify.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final RentalRepository rentalRepository;
    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;

    public AdminService(
            UserRepository userRepository,
            ItemRepository itemRepository,
            RentalRepository rentalRepository,
            ReportRepository reportRepository,
            ReviewRepository reviewRepository
    ) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.rentalRepository = rentalRepository;
        this.reportRepository = reportRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getDashboardStats() {
        return new AdminStatsResponse(
                userRepository.count(),
                userRepository.countByIsSuspendedFalse(),
                userRepository.countByIsSuspendedTrue(),
                itemRepository.count(),
                itemRepository.countByIsAvailableTrue(),
                rentalRepository.count(),
                rentalRepository.countByStatus(RentalStatus.ACTIVE),
                rentalRepository.countByStatus(RentalStatus.COMPLETED),
                reportRepository.countByStatus(ReportStatus.PENDING),
                reportRepository.countByStatus(ReportStatus.RESOLVED),
                reviewRepository.count()
        );
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<UserResponse> getUsers(int page, int limit, String search, UserRole role, Boolean suspended) {
        int validatedPage = Math.max(page, 1);
        int validatedLimit = Math.min(Math.max(limit, 1), 100);

        Pageable pageable = PageRequest.of(validatedPage - 1, validatedLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<User> spec = Specification.where(UserSpecifications.search(search))
                .and(UserSpecifications.hasRole(role))
                .and(UserSpecifications.isSuspended(suspended));

        Page<User> pageResult = userRepository.findAll(spec, pageable);
        List<UserResponse> responses = pageResult.getContent().stream()
                .map(UserResponse::fromEntity)
                .toList();

        return PaginatedResponse.of("Users fetched", responses, validatedPage, validatedLimit, pageResult.getTotalElements());
    }

    @Transactional
    public UserResponse toggleUserSuspension(Long targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.setSuspended(!user.isSuspended());
        User updated = userRepository.save(user);
        return UserResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ItemResponse> getItems(int page, int limit, String search, ItemCategory category, Boolean available) {
        int validatedPage = Math.max(page, 1);
        int validatedLimit = Math.min(Math.max(limit, 1), 100);

        Pageable pageable = PageRequest.of(validatedPage - 1, validatedLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Item> spec = Specification.where(ItemSpecifications.search(search))
                .and(ItemSpecifications.hasCategory(category))
                .and(ItemSpecifications.isAvailable(available));

        Page<Item> pageResult = itemRepository.findAll(spec, pageable);
        List<ItemResponse> responses = pageResult.getContent().stream()
                .map(ItemResponse::fromEntity)
                .toList();

        return PaginatedResponse.of("Items fetched", responses, validatedPage, validatedLimit, pageResult.getTotalElements());
    }

    @Transactional
    public void deleteItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found"));

        boolean hasActiveOrApprovedRentals = itemRepository.hasRentalsWithStatuses(
                itemId,
                List.of(RentalStatus.APPROVED, RentalStatus.ACTIVE)
        );

        if (hasActiveOrApprovedRentals) {
            throw new BadRequestException("Cannot delete item with active or approved rentals");
        }

        itemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<RentalResponse> getRentals(int page, int limit, RentalStatus status) {
        int validatedPage = Math.max(page, 1);
        int validatedLimit = Math.min(Math.max(limit, 1), 100);

        Pageable pageable = PageRequest.of(validatedPage - 1, validatedLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Rental> spec = Specification.where(RentalSpecifications.hasStatus(status));

        Page<Rental> pageResult = rentalRepository.findAll(spec, pageable);
        List<RentalResponse> responses = pageResult.getContent().stream()
                .map(RentalResponse::fromEntity)
                .toList();

        return PaginatedResponse.of("Rentals fetched", responses, validatedPage, validatedLimit, pageResult.getTotalElements());
    }
}
