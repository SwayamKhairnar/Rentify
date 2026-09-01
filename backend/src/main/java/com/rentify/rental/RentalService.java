package com.rentify.rental;

import com.rentify.conversation.Conversation;
import com.rentify.conversation.ConversationRepository;
import com.rentify.exception.BadRequestException;
import com.rentify.exception.ForbiddenException;
import com.rentify.exception.NotFoundException;
import com.rentify.item.Item;
import com.rentify.item.ItemRepository;
import com.rentify.notification.NotificationService;
import com.rentify.notification.NotificationType;
import com.rentify.rental.dto.CreateRentalRequest;
import com.rentify.rental.dto.RentalResponse;
import com.rentify.rental.dto.UpdateRentalStatusRequest;
import com.rentify.user.User;
import com.rentify.user.UserRepository;
import com.rentify.user.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class RentalService {

    private final RentalRepository rentalRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ConversationRepository conversationRepository;

    public RentalService(
            RentalRepository rentalRepository,
            ItemRepository itemRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            ConversationRepository conversationRepository
    ) {
        this.rentalRepository = rentalRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.conversationRepository = conversationRepository;
    }

    @Transactional
    public RentalResponse createRental(Long renterId, CreateRentalRequest request) {
        if (request.startDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Start date cannot be in the past");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        User renter = userRepository.findById(renterId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Item item = itemRepository.findById(request.itemId())
                .orElseThrow(() -> new NotFoundException("Item not found"));

        if (item.getOwner().getId().equals(renterId)) {
            throw new BadRequestException("You cannot rent your own item");
        }

        if (!item.isAvailable()) {
            throw new BadRequestException("Item is currently not available for rent");
        }

        boolean hasOverlap = rentalRepository.hasOverlappingRentals(
                item.getId(),
                request.startDate(),
                request.endDate(),
                List.of(RentalStatus.APPROVED, RentalStatus.ACTIVE)
        );
        if (hasOverlap) {
            throw new BadRequestException("Item is not available for the selected dates");
        }

        long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        BigDecimal totalPrice = item.getPricePerDay().multiply(BigDecimal.valueOf(days));

        Rental rental = new Rental();
        rental.setRenter(renter);
        rental.setItem(item);
        rental.setOwner(item.getOwner());
        rental.setStartDate(request.startDate());
        rental.setEndDate(request.endDate());
        rental.setTotalPrice(totalPrice);
        rental.setStatus(RentalStatus.PENDING);
        rental.setMessage(request.message() != null ? request.message().trim() : "");

        Rental savedRental = rentalRepository.save(rental);

        Conversation conversation = new Conversation(savedRental, renter, item.getOwner());
        conversationRepository.save(conversation);

        notificationService.createNotification(
                item.getOwner(),
                renter,
                NotificationType.RENTAL_REQUEST,
                "New Rental Request",
                renter.getName() + " requested to rent " + item.getTitle(),
                "/rentals/" + savedRental.getId()
        );

        return RentalResponse.fromEntity(savedRental);
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> getMyRentals(Long renterId) {
        List<Rental> rentals = rentalRepository.findByRenterIdOrderByCreatedAtDesc(renterId);
        return rentals.stream()
                .map(RentalResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> getReceivedRentals(Long ownerId) {
        List<Rental> rentals = rentalRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
        return rentals.stream()
                .map(RentalResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public RentalResponse getRentalById(Long userId, Long rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new NotFoundException("Rental not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        boolean isAuthorized = user.getRole() == UserRole.ADMIN
                || rental.getRenter().getId().equals(userId)
                || rental.getOwner().getId().equals(userId);

        if (!isAuthorized) {
            throw new ForbiddenException("You are not authorized to view this rental");
        }

        return RentalResponse.fromEntity(rental);
    }

    @Transactional
    public RentalResponse updateRentalStatus(Long userId, Long rentalId, UpdateRentalStatusRequest request) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new NotFoundException("Rental not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        RentalStatus currentStatus = rental.getStatus();
        RentalStatus newStatus = request.status();

        switch (newStatus) {
            case APPROVED -> {
                if (!rental.getOwner().getId().equals(userId)) {
                    throw new ForbiddenException("Only the item owner can approve rental requests");
                }
                if (currentStatus != RentalStatus.PENDING) {
                    throw new BadRequestException("Can only approve pending rental requests");
                }
                boolean hasOverlap = rentalRepository.hasOverlappingRentals(
                        rental.getItem().getId(),
                        rental.getStartDate(),
                        rental.getEndDate(),
                        List.of(RentalStatus.APPROVED, RentalStatus.ACTIVE)
                );
                if (hasOverlap) {
                    throw new BadRequestException("Cannot approve: another rental already overlaps with these dates");
                }
                rental.setStatus(RentalStatus.APPROVED);
                notificationService.createNotification(
                        rental.getRenter(),
                        user,
                        NotificationType.RENTAL_STATUS,
                        "Rental Request Approved",
                        "Your request to rent " + rental.getItem().getTitle() + " has been approved!",
                        "/rentals/" + rental.getId()
                );
            }
            case REJECTED -> {
                if (!rental.getOwner().getId().equals(userId)) {
                    throw new ForbiddenException("Only the item owner can reject rental requests");
                }
                if (currentStatus != RentalStatus.PENDING) {
                    throw new BadRequestException("Can only reject pending rental requests");
                }
                rental.setStatus(RentalStatus.REJECTED);
                notificationService.createNotification(
                        rental.getRenter(),
                        user,
                        NotificationType.RENTAL_STATUS,
                        "Rental Request Declined",
                        "Your request to rent " + rental.getItem().getTitle() + " was declined.",
                        "/rentals/" + rental.getId()
                );
            }
            case CANCELLED -> {
                boolean isParticipant = rental.getRenter().getId().equals(userId) || rental.getOwner().getId().equals(userId);
                if (!isParticipant) {
                    throw new ForbiddenException("You are not authorized to cancel this rental");
                }
                if (currentStatus != RentalStatus.PENDING && currentStatus != RentalStatus.APPROVED) {
                    throw new BadRequestException("Cannot cancel a rental that is " + currentStatus.getValue());
                }
                rental.setStatus(RentalStatus.CANCELLED);
                User recipient = rental.getRenter().getId().equals(userId) ? rental.getOwner() : rental.getRenter();
                String message = rental.getRenter().getId().equals(userId)
                        ? rental.getRenter().getName() + " cancelled their rental request for " + rental.getItem().getTitle()
                        : "The rental for " + rental.getItem().getTitle() + " was cancelled by the owner.";

                notificationService.createNotification(
                        recipient,
                        user,
                        NotificationType.RENTAL_STATUS,
                        "Rental Cancelled",
                        message,
                        "/rentals/" + rental.getId()
                );
            }
            case ACTIVE -> {
                boolean isParticipant = rental.getRenter().getId().equals(userId) || rental.getOwner().getId().equals(userId);
                if (!isParticipant) {
                    throw new ForbiddenException("You are not authorized to start this rental");
                }
                if (currentStatus != RentalStatus.APPROVED) {
                    throw new BadRequestException("Can only start an approved rental");
                }
                rental.setStatus(RentalStatus.ACTIVE);
                User recipient = rental.getRenter().getId().equals(userId) ? rental.getOwner() : rental.getRenter();
                notificationService.createNotification(
                        recipient,
                        user,
                        NotificationType.RENTAL_STATUS,
                        "Rental Started",
                        "The rental period for " + rental.getItem().getTitle() + " is now active.",
                        "/rentals/" + rental.getId()
                );
            }
            case COMPLETED -> {
                if (!rental.getOwner().getId().equals(userId)) {
                    throw new ForbiddenException("Only the item owner can mark the rental as completed");
                }
                if (currentStatus != RentalStatus.ACTIVE) {
                    throw new BadRequestException("Can only complete an active rental");
                }
                rental.setStatus(RentalStatus.COMPLETED);
                notificationService.createNotification(
                        rental.getRenter(),
                        user,
                        NotificationType.RENTAL_STATUS,
                        "Rental Completed",
                        "The rental for " + rental.getItem().getTitle() + " has been marked as completed. Please leave a review!",
                        "/rentals/" + rental.getId()
                );
            }
            default -> throw new BadRequestException("Invalid status transition");
        }

        if (request.message() != null && !request.message().isBlank()) {
            rental.setMessage(request.message().trim());
        }

        Rental updatedRental = rentalRepository.save(rental);
        return RentalResponse.fromEntity(updatedRental);
    }
}
