package com.rentify.item;

import com.rentify.common.PaginatedResponse;
import com.rentify.exception.BadRequestException;
import com.rentify.exception.ForbiddenException;
import com.rentify.exception.NotFoundException;
import com.rentify.item.dto.CreateItemRequest;
import com.rentify.item.dto.ItemResponse;
import com.rentify.item.dto.UpdateItemRequest;
import com.rentify.rental.Rental;
import com.rentify.rental.RentalRepository;
import com.rentify.rental.RentalStatus;
import com.rentify.user.User;
import com.rentify.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final RentalRepository rentalRepository;

    public ItemService(
            ItemRepository itemRepository,
            UserRepository userRepository,
            RentalRepository rentalRepository
    ) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.rentalRepository = rentalRepository;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ItemResponse> getItems(
            int page,
            int limit,
            String search,
            ItemCategory category,
            ItemCondition condition,
            Long ownerId,
            String sort
    ) {
        int validatedPage = Math.max(page, 1);
        int validatedLimit = Math.min(Math.max(limit, 1), 50);

        Sort sortOrder = switch (sort != null ? sort.toLowerCase() : "") {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "pricePerDay");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "pricePerDay");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(validatedPage - 1, validatedLimit, sortOrder);

        Specification<Item> spec = Specification
                .where(ItemSpecifications.isAvailable(true))
                .and(ItemSpecifications.hasCategory(category))
                .and(ItemSpecifications.hasCondition(condition))
                .and(ItemSpecifications.hasOwner(ownerId))
                .and(ItemSpecifications.search(search));

        Page<Item> pageResult = itemRepository.findAll(spec, pageable);
        List<ItemResponse> itemResponses = pageResult.getContent().stream()
                .map(ItemResponse::fromEntity)
                .toList();

        return PaginatedResponse.of("Items fetched", itemResponses, validatedPage, validatedLimit, pageResult.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getMyItems(Long userId) {
        List<Item> items = itemRepository.findByOwnerIdOrderByCreatedAtDesc(userId);
        return items.stream()
                .map(ItemResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ItemResponse getItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        return ItemResponse.fromEntity(item);
    }

    @Transactional
    public ItemResponse createItem(Long userId, CreateItemRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Item item = new Item();
        item.setOwner(user);
        item.setTitle(request.title().trim());
        item.setDescription(request.description().trim());
        item.setCategory(request.category());
        item.setPricePerDay(request.pricePerDay());
        item.setCondition(request.condition() != null ? request.condition() : ItemCondition.GOOD);
        item.setLocation(request.location() != null ? request.location().trim() : "");
        item.setAvailable(true);

        if (request.images() != null) {
            int count = 0;
            for (String imageUrl : request.images()) {
                if (count >= 5) break;
                if (imageUrl != null && !imageUrl.isBlank()) {
                    item.addImage(imageUrl.trim());
                    count++;
                }
            }
        }

        Item savedItem = itemRepository.save(item);
        return ItemResponse.fromEntity(savedItem);
    }

    @Transactional
    public ItemResponse updateItem(Long userId, Long itemId, UpdateItemRequest request) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found"));

        if (!item.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to update this item");
        }

        if (request.title() != null && !request.title().isBlank()) {
            item.setTitle(request.title().trim());
        }
        if (request.description() != null && !request.description().isBlank()) {
            item.setDescription(request.description().trim());
        }
        if (request.category() != null) {
            item.setCategory(request.category());
        }
        if (request.pricePerDay() != null) {
            item.setPricePerDay(request.pricePerDay());
        }
        if (request.condition() != null) {
            item.setCondition(request.condition());
        }
        if (request.location() != null) {
            item.setLocation(request.location().trim());
        }
        if (request.isAvailable() != null) {
            item.setAvailable(request.isAvailable());
        }
        if (request.images() != null) {
            item.getImages().clear();
            int count = 0;
            for (String imageUrl : request.images()) {
                if (count >= 5) break;
                if (imageUrl != null && !imageUrl.isBlank()) {
                    item.addImage(imageUrl.trim());
                    count++;
                }
            }
        }

        Item updatedItem = itemRepository.save(item);
        return ItemResponse.fromEntity(updatedItem);
    }

    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found"));

        if (!item.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to delete this item");
        }

        boolean hasActiveRentals = itemRepository.hasRentalsWithStatuses(
                itemId,
                List.of(RentalStatus.APPROVED, RentalStatus.ACTIVE)
        );
        if (hasActiveRentals) {
            throw new BadRequestException("Cannot delete item with active or approved rentals");
        }

        List<Rental> rentals = rentalRepository.findByItemId(itemId);
        for (Rental rental : rentals) {
            if (rental.getStatus() == RentalStatus.PENDING) {
                rental.setStatus(RentalStatus.CANCELLED);
                rental.setMessage("The item has been deleted by the owner.");
                rentalRepository.save(rental);
            }
        }

        itemRepository.delete(item);
    }
}
