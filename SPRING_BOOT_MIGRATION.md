# Rentify Spring Boot Migration Plan

**Project:** Rentify Campus Rental Platform  
**Current backend:** Node.js, Express.js, MongoDB, Mongoose  
**Target backend:** Java, Spring Boot, Spring Data JPA/Hibernate, PostgreSQL, Spring Security, JWT  
**Frontend goal:** Keep the existing React/Vite frontend largely unchanged by preserving endpoint paths, request bodies, response envelopes, token behavior, and error behavior wherever practical.

---

## 1. Executive Summary

Rentify is a campus-focused peer-to-peer rental platform where students list items, request rentals, chat in rental-specific conversations, review each other after completed rentals, receive notifications, and file dispute reports that admins can moderate.

The correct target architecture for this project is a simple Spring Boot monolith backed by PostgreSQL. This is the best fit because the app is a single cohesive domain, has strong relational data, needs transactional consistency for rentals/reviews/admin cleanup, and should remain easy to explain in an interview.

Do not introduce microservices, Kafka, Redis, Elasticsearch, CQRS, event sourcing, Kubernetes, or other infrastructure that is not necessary for this application.

The migration should prioritize:

1. Preserving the React frontend contract.
2. Replacing MongoDB document models with normalized PostgreSQL tables.
3. Replacing Mongoose services with transactional Spring services.
4. Replacing Express middleware with Spring Security, validation, and controller advice.
5. Making rental overlap prevention and state transitions reliable.
6. Keeping the implementation understandable and demonstrable.

---

## 2. Non-Negotiable Architecture Rules

- Build one Spring Boot backend application.
- Keep it as a layered monolith:
  - Controller
  - DTO
  - Service
  - Repository
  - Entity
  - Security
  - Exception/configuration
- Preserve `/api/...` endpoint paths unless there is a strong reason to change one.
- Preserve response envelopes:

```json
{
  "success": true,
  "message": "Operation message",
  "data": {}
}
```

```json
{
  "success": true,
  "message": "Items fetched",
  "data": [],
  "pagination": {
    "page": 1,
    "limit": 12,
    "total": 45,
    "pages": 4
  }
}
```

```json
{
  "success": false,
  "message": "Descriptive error message"
}
```

- Keep JWT in the `Authorization: Bearer <token>` header.
- Keep the frontend localStorage key unchanged: `rentify_token`.
- Keep suspended users blocked on the next authenticated API call.
- Keep Cloudinary as the image provider.
- Keep polling-based chat/notifications for now. Do not add WebSockets in the migration.
- Keep payments out of scope. Existing prices and offer prices remain record-keeping fields only.

---

## 3. Target Tech Stack

Use this stack unless the existing project setup strongly requires different versions:

| Concern | Target |
|---|---|
| Language | Java 21, or Java 17 if deployment platform requires it |
| Framework | Spring Boot 3.x |
| Web | Spring Web MVC |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 15+ |
| Migrations | Flyway |
| Security | Spring Security |
| JWT | `io.jsonwebtoken:jjwt-*` or `com.auth0:java-jwt` |
| Password hashing | Spring `BCryptPasswordEncoder` strength 12 |
| Validation | Jakarta Bean Validation |
| File upload | Spring `MultipartFile` |
| Cloud media | Cloudinary Java SDK |
| Boilerplate reduction | Lombok optional; prefer explicit code if team/interview clarity matters |
| Testing | JUnit 5, Spring Boot Test, MockMvc, Testcontainers PostgreSQL |
| Build | Maven or Gradle; choose the one Antigravity can maintain consistently |

Recommended dependencies:

- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-security`
- `spring-boot-starter-data-jpa`
- `org.postgresql:postgresql`
- `org.flywaydb:flyway-core`
- `org.flywaydb:flyway-database-postgresql`
- `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson`
- `org.springframework.boot:spring-boot-starter-test`
- `org.testcontainers:postgresql`
- `com.cloudinary:cloudinary-http5`

Optional:

- `springdoc-openapi-starter-webmvc-ui` for API documentation.
- `bucket4j` only if rate limiting is required immediately. Otherwise document it as future hardening.

---

## 4. Recommended Project Structure

Use package root:

```text
com.rentify
```

Recommended structure:

```text
src/main/java/com/rentify/
├── RentifyApplication.java
├── admin/
│   ├── AdminController.java
│   └── AdminService.java
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   └── AuthResponse.java
│   └── security/
│       ├── CustomUserDetails.java
│       ├── CustomUserDetailsService.java
│       ├── JwtAuthenticationFilter.java
│       ├── JwtService.java
│       └── SecurityConfig.java
├── common/
│   ├── ApiResponse.java
│   ├── PaginatedResponse.java
│   ├── ErrorResponse.java
│   ├── PageMapper.java
│   └── CurrentUser.java
├── config/
│   ├── CloudinaryConfig.java
│   ├── JacksonConfig.java
│   └── JpaConfig.java
├── conversation/
│   ├── ChatController.java
│   ├── ChatService.java
│   ├── Conversation.java
│   ├── ConversationRepository.java
│   ├── Message.java
│   ├── MessageRepository.java
│   └── dto/
├── exception/
│   ├── ApiException.java
│   ├── BadRequestException.java
│   ├── ConflictException.java
│   ├── ForbiddenException.java
│   ├── NotFoundException.java
│   ├── UnauthorizedException.java
│   └── GlobalExceptionHandler.java
├── item/
│   ├── Item.java
│   ├── ItemImage.java
│   ├── ItemController.java
│   ├── ItemRepository.java
│   ├── ItemService.java
│   ├── ItemSpecifications.java
│   └── dto/
├── notification/
│   ├── Notification.java
│   ├── NotificationController.java
│   ├── NotificationRepository.java
│   ├── NotificationService.java
│   └── dto/
├── rental/
│   ├── Rental.java
│   ├── RentalController.java
│   ├── RentalRepository.java
│   ├── RentalService.java
│   ├── RentalStatus.java
│   └── dto/
├── report/
│   ├── Report.java
│   ├── ReportController.java
│   ├── ReportRepository.java
│   ├── ReportService.java
│   └── dto/
├── review/
│   ├── Review.java
│   ├── ReviewController.java
│   ├── ReviewRepository.java
│   ├── ReviewService.java
│   └── dto/
├── upload/
│   ├── UploadController.java
│   └── CloudinaryService.java
└── user/
    ├── User.java
    ├── UserController.java
    ├── UserRepository.java
    ├── UserService.java
    └── dto/
```

This package-by-feature structure is easier to navigate than a large global `controller/service/repository` structure while still being simple.

---

## 5. Domain Model and Entity Count

The current Mongo backend has 8 primary domain entities. Preserve them:

1. User
2. Item
3. Rental
4. Conversation
5. Message
6. Review
7. Notification
8. Report

PostgreSQL also needs supporting relational tables:

- `item_images`
- `conversation_participants`

These are implementation tables, not separate business entities.

---

## 6. ID Strategy

Use `BIGSERIAL` / `Long` IDs for the interview project.

Reason:

- Easier to read in demos.
- Easier to use in JPA relationships.
- Fully adequate for a campus rental app.
- Existing React route params are strings already, so numeric IDs still pass through URLs cleanly.

Important frontend implication:

- Mongo ObjectIds such as `"66abc..."` become numeric JSON values like `1`.
- If the frontend treats IDs only as route params and equality values, this should work.
- If the frontend assumes Mongo-specific `_id`, adapt backend DTOs to expose `_id` aliases if required.

Recommended compatibility approach:

- Internally use `id`.
- In API response DTOs, include both `id` and `_id` during the migration if the React frontend currently reads `_id`.

Example:

```json
{
  "id": 12,
  "_id": 12,
  "title": "Scientific Calculator"
}
```

This avoids unnecessary frontend rewrites. Later, the frontend can be cleaned up to use only `id`.

---

## 7. PostgreSQL Schema

Implement the schema with Flyway. Create `src/main/resources/db/migration/V1__initial_schema.sql`.

Use `DATE` for rental start/end dates to avoid timezone off-by-one issues. Use `TIMESTAMPTZ` for created/updated timestamps.

### 7.1 Users

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'student',
    password VARCHAR(255) NOT NULL,
    avatar VARCHAR(500) NOT NULL DEFAULT '',
    campus VARCHAR(100) NOT NULL DEFAULT '',
    bio VARCHAR(300) NOT NULL DEFAULT '',
    phone VARCHAR(20) NOT NULL DEFAULT '',
    rating NUMERIC(3,1) NOT NULL DEFAULT 0.0,
    total_reviews INTEGER NOT NULL DEFAULT 0,
    lender_rating NUMERIC(3,1) NOT NULL DEFAULT 0.0,
    total_lender_reviews INTEGER NOT NULL DEFAULT 0,
    renter_rating NUMERIC(3,1) NOT NULL DEFAULT 0.0,
    total_renter_reviews INTEGER NOT NULL DEFAULT 0,
    item_quality_average NUMERIC(3,1) NOT NULL DEFAULT 0.0,
    total_item_quality_reviews INTEGER NOT NULL DEFAULT 0,
    is_suspended BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_role CHECK (role IN ('student', 'admin')),
    CONSTRAINT ck_users_rating CHECK (rating >= 0 AND rating <= 5),
    CONSTRAINT ck_users_lender_rating CHECK (lender_rating >= 0 AND lender_rating <= 5),
    CONSTRAINT ck_users_renter_rating CHECK (renter_rating >= 0 AND renter_rating <= 5),
    CONSTRAINT ck_users_item_quality_average CHECK (item_quality_average >= 0 AND item_quality_average <= 5)
);

CREATE INDEX idx_users_created_at ON users (created_at DESC);
```

### 7.2 Items

```sql
CREATE TABLE items (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    category VARCHAR(30) NOT NULL,
    price_per_day NUMERIC(10,2) NOT NULL,
    condition VARCHAR(20) NOT NULL DEFAULT 'good',
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    location VARCHAR(200) NOT NULL DEFAULT '',
    rating NUMERIC(3,1) NOT NULL DEFAULT 0.0,
    total_reviews INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_items_category CHECK (category IN ('textbooks', 'electronics', 'bikes', 'cameras', 'furniture', 'clothing', 'sports', 'instruments', 'other')),
    CONSTRAINT ck_items_condition CHECK (condition IN ('new', 'like-new', 'good', 'fair', 'poor')),
    CONSTRAINT ck_items_price CHECK (price_per_day >= 0),
    CONSTRAINT ck_items_rating CHECK (rating >= 0 AND rating <= 5)
);

CREATE INDEX idx_items_owner ON items(owner_id);
CREATE INDEX idx_items_category ON items(category);
CREATE INDEX idx_items_condition ON items(condition);
CREATE INDEX idx_items_available ON items(is_available);
CREATE INDEX idx_items_created_at ON items(created_at DESC);
```

### 7.3 Item Images

```sql
CREATE TABLE item_images (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_item_images_item ON item_images(item_id);
```

JPA mapping option:

- Use `@ElementCollection` for simpler code, or use `ItemImage` entity for explicit ordering.
- Prefer `ItemImage` entity because it maps cleanly to `display_order` and future deletion cleanup.

### 7.4 Rentals

```sql
CREATE TABLE rentals (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    renter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_price NUMERIC(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    message VARCHAR(500) NOT NULL DEFAULT '',
    offer_price NUMERIC(10,2),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_rentals_dates CHECK (end_date > start_date),
    CONSTRAINT ck_rentals_total_price CHECK (total_price >= 0),
    CONSTRAINT ck_rentals_offer_price CHECK (offer_price IS NULL OR offer_price >= 0),
    CONSTRAINT ck_rentals_status CHECK (status IN ('pending', 'approved', 'rejected', 'active', 'completed', 'cancelled'))
);

CREATE INDEX idx_rentals_renter_status ON rentals(renter_id, status);
CREATE INDEX idx_rentals_owner_status ON rentals(owner_id, status);
CREATE INDEX idx_rentals_item ON rentals(item_id);
CREATE INDEX idx_rentals_item_status_dates ON rentals(item_id, status, start_date, end_date);
CREATE INDEX idx_rentals_created_at ON rentals(created_at DESC);
```

### 7.5 Conversations and Participants

```sql
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    rental_id BIGINT NOT NULL UNIQUE REFERENCES rentals(id) ON DELETE CASCADE,
    last_message VARCHAR(100) NOT NULL DEFAULT '',
    last_message_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE conversation_participants (
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX idx_conversation_participants_user ON conversation_participants(user_id);
CREATE INDEX idx_conversations_last_message_at ON conversations(last_message_at DESC);
```

### 7.6 Messages

```sql
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content VARCHAR(2000) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_conversation_created ON messages(conversation_id, created_at);
CREATE INDEX idx_messages_unread ON messages(conversation_id, sender_id, is_read);
```

### 7.7 Reviews

```sql
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    rental_id BIGINT NOT NULL REFERENCES rentals(id) ON DELETE CASCADE,
    reviewer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reviewee_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL,
    item_rating INTEGER,
    type VARCHAR(20) NOT NULL,
    comment VARCHAR(500) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_reviews_rental_reviewer UNIQUE (rental_id, reviewer_id),
    CONSTRAINT ck_reviews_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT ck_reviews_item_rating CHECK (item_rating IS NULL OR (item_rating >= 1 AND item_rating <= 5)),
    CONSTRAINT ck_reviews_type CHECK (type IN ('lender', 'renter'))
);

CREATE INDEX idx_reviews_reviewee ON reviews(reviewee_id);
CREATE INDEX idx_reviews_rental ON reviews(rental_id);
CREATE INDEX idx_reviews_created_at ON reviews(created_at DESC);
```

### 7.8 Notifications

```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    link VARCHAR(255) NOT NULL DEFAULT '',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_notifications_type CHECK (type IN ('rental_request', 'rental_status', 'review_received', 'message', 'system'))
);

CREATE INDEX idx_notifications_recipient_read ON notifications(recipient_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
```

### 7.9 Reports

```sql
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reported_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rental_id BIGINT NOT NULL REFERENCES rentals(id) ON DELETE CASCADE,
    reason VARCHAR(50) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    evidence_image VARCHAR(500) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    admin_notes TEXT NOT NULL DEFAULT '',
    admin_action VARCHAR(30) NOT NULL DEFAULT 'none',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_reports_reason CHECK (reason IN ('Late Return', 'Item Damage', 'Fake Product/Description', 'Inappropriate Behavior', 'Payment Issues', 'No Show', 'Other')),
    CONSTRAINT ck_reports_status CHECK (status IN ('pending', 'reviewed', 'resolved', 'dismissed')),
    CONSTRAINT ck_reports_admin_action CHECK (admin_action IN ('none', 'warned', 'listing_removed', 'account_suspended', 'resolved'))
);

CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_reported_user ON reports(reported_user_id);
CREATE INDEX idx_reports_created_at ON reports(created_at DESC);
```

### 7.10 Updated Timestamp Strategy

Use JPA auditing:

- Add `@EnableJpaAuditing`.
- Use `@CreatedDate` and `@LastModifiedDate`.
- Use `Instant` for audit timestamps.

Do not rely only on database defaults because JPA responses should have timestamps immediately after persistence.

---

## 8. JPA Entity Design

### 8.1 Common Entity Rules

- Use `Long id`.
- Use enums in Java with `@Enumerated(EnumType.STRING)`.
- Use `BigDecimal` for money.
- Use `LocalDate` for `Rental.startDate` and `Rental.endDate`.
- Use `Instant` for created/updated timestamps.
- Add `@Version` to `User`, `Item`, and `Rental`.
- Do not expose entities directly from controllers.
- Use DTOs for all API responses.
- Avoid eager loading by default. Use fetch joins or projections for endpoints that need nested data.

### 8.2 Enums

Create enums:

```text
UserRole: student, admin
ItemCategory: textbooks, electronics, bikes, cameras, furniture, clothing, sports, instruments, other
ItemCondition: new, like-new, good, fair, poor
RentalStatus: pending, approved, rejected, active, completed, cancelled
ReviewType: lender, renter
NotificationType: rental_request, rental_status, review_received, message, system
ReportReason: Late Return, Item Damage, Fake Product/Description, Inappropriate Behavior, Payment Issues, No Show, Other
ReportStatus: pending, reviewed, resolved, dismissed
AdminAction: none, warned, listing_removed, account_suspended, resolved
```

Java enum names should be conventional uppercase, but JSON values must remain frontend-compatible. Use `@JsonValue` and `@JsonCreator` if needed so values serialize as the existing lowercase or spaced strings.

Example:

```java
public enum RentalStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    ACTIVE("active"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }
}
```

### 8.3 Relationship Mapping

Use these relationships:

- `User` one-to-many `Item` as owner.
- `User` one-to-many `Rental` as renter.
- `User` one-to-many `Rental` as owner.
- `Item` one-to-many `Rental`.
- `Item` one-to-many `ItemImage`, ordered by `displayOrder`.
- `Rental` one-to-one `Conversation`.
- `Conversation` many-to-many `User` through `conversation_participants`.
- `Conversation` one-to-many `Message`.
- `Message` many-to-one `User` as sender.
- `Rental` one-to-many `Review`.
- `Review` many-to-one `User` as reviewer.
- `Review` many-to-one `User` as reviewee.
- `Notification` many-to-one `User` as recipient and nullable sender.
- `Report` many-to-one `User` as reporter and reported user.
- `Report` many-to-one `Rental`.

Keep cascade behavior conservative in JPA:

- Allow database foreign keys to enforce deletion.
- Avoid broad `CascadeType.REMOVE` from `User` across the whole graph unless the service explicitly handles delete ordering.
- Use explicit delete repository methods for admin deletion so behavior is visible.

---

## 9. MongoDB to PostgreSQL Mapping

| Mongo/Mongoose Pattern | Spring Boot/PostgreSQL Replacement |
|---|---|
| `_id: ObjectId` | `id BIGSERIAL`, `Long id`; optionally expose `_id` in DTOs |
| Document references + `.populate()` | JPA relationships plus DTO mapping/fetch joins |
| `images: [String]` | `item_images` child table |
| `participants: [ObjectId]` | `conversation_participants` join table |
| Zod validation | Jakarta Bean Validation on request DTOs |
| Mongoose pre-save password hook | Explicit password hashing in `AuthService.register` |
| Mongo text index | JPA Specification with `LOWER(...) LIKE`, optionally PostgreSQL full-text later |
| Aggregation pipeline rating updates | Transactional service methods with optimistic locking or JPQL arithmetic updates |
| Mongoose sessions | `@Transactional` service methods |
| Express `authenticate` | Spring Security JWT filter |
| Express `authorizeAdmin` | `@PreAuthorize("hasRole('ADMIN')")` or endpoint matcher |
| `ApiError` | Custom runtime exceptions + `@RestControllerAdvice` |
| Multer upload | Spring multipart endpoint + Cloudinary Java SDK |

---

## 10. Repositories and Required Queries

### 10.1 UserRepository

Required methods:

```java
Optional<User> findByEmailIgnoreCase(String email);
boolean existsByEmailIgnoreCase(String email);
List<User> findByRole(UserRole role);
```

Admin listing:

```java
Page<User> findAll(Pageable pageable);
```

### 10.2 ItemRepository

Required:

```java
Page<Item> findAll(Specification<Item> spec, Pageable pageable);
List<Item> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
```

Deletion guard:

```java
@Query("""
    select count(r) > 0
    from Rental r
    where r.item.id = :itemId
      and r.status in :statuses
""")
boolean hasRentalsWithStatuses(Long itemId, Collection<RentalStatus> statuses);
```

For simple search, use `JpaSpecificationExecutor<Item>`:

- `isAvailable = true`
- optional owner
- optional category
- optional condition
- optional search:
  - `lower(title) like %search%`
  - OR `lower(description) like %search%`

This is enough for an interview project. PostgreSQL full-text search can be a future improvement.

### 10.3 RentalRepository

Overlap check:

```java
@Query("""
    select r
    from Rental r
    where r.item.id = :itemId
      and r.status in :statuses
      and :startDate <= r.endDate
      and :endDate >= r.startDate
""")
List<Rental> findOverlappingRentals(
    Long itemId,
    LocalDate startDate,
    LocalDate endDate,
    Collection<RentalStatus> statuses
);
```

Lock item-related rentals during approval:

```java
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
    Long itemId,
    LocalDate startDate,
    LocalDate endDate,
    Collection<RentalStatus> statuses
);
```

User rental lists:

```java
List<Rental> findByRenterIdOrderByCreatedAtDesc(Long renterId);
List<Rental> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
```

Pending auto-cancel:

```java
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
    Long itemId,
    Long approvedRentalId,
    LocalDate startDate,
    LocalDate endDate
);
```

### 10.4 ConversationRepository

```java
Optional<Conversation> findByRentalId(Long rentalId);

@Query("""
    select distinct c
    from Conversation c
    join c.participants p
    where p.id = :userId
    order by c.lastMessageAt desc
""")
List<Conversation> findUserConversations(Long userId);

@Query("""
    select count(c) > 0
    from Conversation c
    join c.participants p
    where c.id = :conversationId and p.id = :userId
""")
boolean isParticipant(Long conversationId, Long userId);
```

### 10.5 MessageRepository

```java
List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

@Query("""
    select count(m)
    from Message m
    where m.conversation.id in (
        select c.id
        from Conversation c
        join c.participants p
        where p.id = :userId
    )
    and m.sender.id <> :userId
    and m.isRead = false
""")
long countUnreadForUser(Long userId);

@Modifying
@Query("""
    update Message m
    set m.isRead = true
    where m.conversation.id = :conversationId
      and m.sender.id <> :userId
      and m.isRead = false
""")
int markConversationMessagesRead(Long conversationId, Long userId);
```

### 10.6 ReviewRepository

```java
boolean existsByRentalIdAndReviewerId(Long rentalId, Long reviewerId);
List<Review> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId);
List<Review> findByRentalIdOrderByCreatedAtDesc(Long rentalId);
```

### 10.7 NotificationRepository

```java
List<Notification> findTop50ByRecipientIdOrderByCreatedAtDesc(Long recipientId);
long countByRecipientIdAndIsReadFalse(Long recipientId);
Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

@Modifying
@Query("update Notification n set n.isRead = true where n.recipient.id = :userId")
int markAllRead(Long userId);
```

### 10.8 ReportRepository

```java
List<Report> findAllByOrderByCreatedAtDesc();
List<Report> findByReporterIdOrReportedUserId(Long reporterId, Long reportedUserId);
```

---

## 11. Service Layer Responsibilities

### 11.1 AuthService

Responsibilities:

- Register user.
- Normalize email to lowercase.
- Reject duplicate email with HTTP 409.
- Hash password using `BCryptPasswordEncoder`.
- Create default student role.
- Login user.
- Check password.
- Reject suspended account with HTTP 403.
- Issue JWT with `{ userId }`.
- Return user DTO and token.

Do not hash passwords in entity callbacks. Keep it explicit in `AuthService` because it is easier to explain and test.

### 11.2 UserService

Responsibilities:

- Fetch public profile by ID.
- Update current user profile.
- Allow only:
  - `name`
  - `bio`
  - `campus`
  - `phone`
  - `avatar`
- Never allow profile update to mutate:
  - `role`
  - `rating`
  - `totalReviews`
  - `isSuspended`
  - password

Rating helper responsibilities:

- Update composite user rating.
- Update lender rating.
- Update renter rating.
- Update item quality average.

Recommended implementation:

- Load the target `User` with optimistic lock.
- Apply rolling average formula in a transaction.
- Save.

Formula:

```text
newAverage = roundToOneDecimal(((currentAverage * currentCount) + newRating) / (currentCount + 1))
newCount = currentCount + 1
```

### 11.3 ItemService

Responsibilities:

- Public item list with pagination/search/filter/sort.
- Current user's items.
- Item detail.
- Create item.
- Update item as owner only.
- Delete item as owner only.

Deletion rule:

- If item has `approved` or `active` rentals, reject with HTTP 400.
- Otherwise, inside a transaction:
  - cancel all pending rentals for that item
  - delete associated conversations/messages through cascade or explicit methods
  - delete item
- Cloudinary deletion should be handled carefully:
  - Collect image URLs first.
  - Commit the database transaction.
  - Delete Cloudinary images after commit.
  - If Cloudinary deletion fails, log it and continue. Do not roll back database deletion after the item is already deleted.

### 11.4 RentalService

Responsibilities:

- Create rental request.
- Fetch outgoing rentals.
- Fetch incoming rentals.
- Fetch rental detail for participants only.
- Update rental status with state machine validation.
- Create conversation on rental creation.
- Create notifications on rental lifecycle events.
- Prevent date overlaps for approved/active rentals.
- Auto-cancel overlapping pending rentals when approving.

Key transaction boundaries:

- `createRentalRequest` must be `@Transactional`.
- `updateRentalStatus` must be `@Transactional`.

Rental creation:

1. Load item and owner.
2. Verify item exists and is available.
3. Reject self-rental.
4. Validate dates as `LocalDate`.
5. Check no overlap with existing `approved` or `active` rentals.
6. Calculate number of days with `ChronoUnit.DAYS.between(startDate, endDate)`.
7. Calculate `totalPrice = days * item.pricePerDay`.
8. Save rental with status `pending`.
9. Create conversation with owner and renter.
10. Send owner notification.

Status update:

- `pending -> approved`: owner only.
- `pending -> rejected`: owner only.
- `pending -> cancelled`: owner or renter.
- `approved -> active`: owner only.
- `approved -> cancelled`: owner or renter.
- `active -> completed`: owner only.
- `active -> cancelled`: owner or renter.

Terminal statuses:

- `completed`, `rejected`, and `cancelled` should not transition to another status.

### 11.5 ChatService

Responsibilities:

- List conversations where current user is a participant.
- Load messages for conversation after participant check.
- Mark messages from the other participant as read when viewing conversation.
- Send message after participant check.
- Update conversation last message snippet and timestamp.
- Count unread messages across all current user's conversations.

Message notification:

- Existing audit identifies notification type `message`, but current endpoint summary only mentions conversation update.
- Implement message notification if the frontend expects it. Otherwise keep it internal-compatible by creating a `message` notification to the other participant with link `/chat`.

### 11.6 ReviewService

Responsibilities:

- Create review for completed rental.
- Enforce participant-only review.
- Enforce one review per rental per reviewer.
- Determine review type:
  - If reviewer is renter, type is `lender`; reviewee is owner.
  - If reviewer is owner, type is `renter`; reviewee is renter.
- Update rating aggregates transactionally.
- Notify reviewee.
- Fetch reviews by user.
- Fetch reviews by rental.

Rating update behavior:

- Always update reviewee composite `rating`.
- If renter reviews owner:
  - update owner `lenderRating`
  - if `itemRating` exists:
    - update owner `itemQualityAverage`
    - update item `rating`
- If owner reviews renter:
  - update renter `renterRating`

### 11.7 NotificationService

Responsibilities:

- Create notification.
- Broadcast admin notifications.
- List latest 50 notifications for current user.
- Count unread.
- Mark one read.
- Mark all read.

This should remain a simple database-backed notification service. No WebSocket/SSE for this migration.

### 11.8 ReportService

Responsibilities:

- Submit report.
- Validate rental exists.
- Validate rental status is `completed`.
- Validate reporter is either renter or owner.
- Validate reported user is the opposing participant.
- Save report.
- Notify all admins.
- Admin list reports.
- Admin respond to report.
- Apply admin action:
  - `warned`: notification to reported user and/or reporter.
  - `listing_removed`: remove item linked to rental if deletion rules allow; for moderation, allow forced removal.
  - `account_suspended`: set reported user `isSuspended = true`.
  - `resolved`: mark report resolved and notify reporter.

### 11.9 AdminService

Responsibilities:

- List all users.
- Delete a user and associated data.

Nuclear deletion behavior:

1. Reject deleting self if desired. This is recommended to avoid locking out the only admin accidentally.
2. Load target user.
3. Collect Cloudinary URLs:
   - images for owned items
   - report evidence images where user is reporter or reported user
4. Inside one transaction:
   - delete reports involving user
   - delete notifications where user is recipient or sender
   - delete reviews where user is reviewer or reviewee
   - delete messages sent by user or messages in conversations involving user
   - delete conversations involving user
   - cancel or delete rentals involving user based on current behavior
   - delete items owned by user
   - delete user
5. After successful commit:
   - delete collected Cloudinary images

Because the current audit says admin deletion purges associated data, the Spring version should delete records rather than merely anonymizing them.

---

## 12. Rental Overlap and Concurrency

### 12.1 Overlap Rule

Two rentals overlap when:

```text
requestedStart <= existingEnd AND requestedEnd >= existingStart
```

This simpler expression is equivalent to the longer Mongo condition and is easier to explain.

### 12.2 Statuses That Block New Rentals

Only these statuses block a new rental request:

- `approved`
- `active`

Pending requests do not block each other. Multiple students may request overlapping dates until the owner approves one.

### 12.3 Approval Concurrency Risk

Risk:

- Two approval requests for overlapping rentals could be processed at the same time.
- Both could pass the overlap check before either commits.

Recommended simple solution:

- Use `@Transactional`.
- During approval, load the target rental with `PESSIMISTIC_WRITE`.
- Also lock overlapping rentals for the same item/date range with `PESSIMISTIC_WRITE`.
- Re-check conflicts inside the lock.
- Approve exactly one rental.
- Auto-cancel conflicting pending rentals.

This is interview-defensible because it uses database row locking rather than external infrastructure.

### 12.4 Optional Stronger PostgreSQL Constraint

If Antigravity has time, add an exclusion constraint to prevent overlapping `approved` and `active` rentals at database level. This is more advanced and not required for the first implementation.

Example concept:

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE rentals
ADD CONSTRAINT no_overlapping_active_bookings
EXCLUDE USING gist (
    item_id WITH =,
    daterange(start_date, end_date, '[]') WITH &&
)
WHERE (status IN ('approved', 'active'));
```

This is a strong interview talking point, but it can be deferred if it slows implementation.

---

## 13. Transactions

Use transactions in these service methods:

| Service method | Why transactional |
|---|---|
| `AuthService.register` | User creation should be atomic |
| `ItemService.deleteItem` | Delete/cancel related item data consistently |
| `RentalService.createRental` | Rental, conversation, and notification should be created together |
| `RentalService.updateStatus` | Status changes, auto-cancellations, and notifications belong together |
| `ChatService.sendMessage` | Message creation and conversation summary update belong together |
| `ChatService.getMessages` | Mark-read update and message fetch should be consistent |
| `ReviewService.createReview` | Review, ratings, and notification must be consistent |
| `ReportService.createReport` | Report and admin notifications belong together |
| `ReportService.respondToReport` | Report status/action and moderation effect belong together |
| `AdminService.deleteUser` | Cascade cleanup must be atomic |

Cloudinary calls should generally not happen inside database transactions unless absolutely necessary.

Preferred Cloudinary rule:

- Upload first, then save URLs.
- Delete remote images after DB commit.
- Log cleanup failures.

---

## 14. Security Design

### 14.1 JWT

JWT payload:

```json
{
  "userId": 123
}
```

Recommended expiry:

- Keep 7 days to match the current backend.

Configuration:

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
    expires-in: ${JWT_EXPIRES_IN:7d}
```

Security filter behavior:

1. Read `Authorization` header.
2. Require `Bearer ` prefix.
3. Validate signature and expiry.
4. Extract `userId`.
5. Load user from database.
6. If not found, reject 401.
7. If `isSuspended = true`, reject 403 with message:
   - `Your account has been suspended by an administrator.`
8. Attach authenticated principal.

### 14.2 Authorization

Public:

- `GET /api/health`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/users/{id}`
- `GET /api/items`
- `GET /api/items/{id}`
- `GET /api/reviews/user/{userId}`
- `GET /api/reviews/rental/{rentalId}`

Authenticated:

- `GET /api/auth/me`
- `PUT /api/users/profile`
- `GET /api/items/mine`
- `POST /api/items`
- `PUT /api/items/{id}`
- `DELETE /api/items/{id}`
- all rental endpoints
- all chat endpoints
- `POST /api/reviews`
- upload endpoint
- notification endpoints
- `POST /api/reports`

Admin only:

- `GET /api/admin/users`
- `DELETE /api/admin/users/{id}`
- `GET /api/reports/admin`
- `PATCH /api/reports/admin/{id}/respond`

### 14.3 CORS

Allow frontend dev origin:

- `http://localhost:5173`

Allow deployment frontend origin through environment variable.

### 14.4 Passwords

- Minimum length: 6 to preserve frontend behavior.
- Use BCrypt strength 12.
- Never return `password` in DTOs.

---

## 15. DTO and API Compatibility

### 15.1 Response Wrappers

Create:

```java
public record ApiResponse<T>(
    boolean success,
    String message,
    T data
) {}
```

```java
public record PaginatedResponse<T>(
    boolean success,
    String message,
    List<T> data,
    Pagination pagination
) {}
```

```java
public record Pagination(
    int page,
    int limit,
    long total,
    int pages
) {}
```

For responses without data, either return:

```json
{ "success": true, "message": "Item deleted" }
```

or include `"data": null` only if the frontend tolerates it. Prefer matching current behavior.

### 15.2 Core DTOs

Create response DTOs:

- `UserResponse`
- `ItemResponse`
- `RentalResponse`
- `ConversationResponse`
- `MessageResponse`
- `ReviewResponse`
- `NotificationResponse`
- `ReportResponse`

DTOs should include nested lightweight summaries where the frontend currently expects populated objects.

Examples:

- Item response includes owner summary.
- Rental response includes item, renter, and owner summaries.
- Report response includes reporter, reported user, and rental summary.
- Conversation response includes participants and rental summary.

### 15.3 `_id` Compatibility

Because the existing React frontend likely expects Mongo `_id`, DTOs should temporarily expose:

```java
@JsonProperty("_id")
public Long mongoCompatibleId() {
    return id;
}
```

This should be done on major response DTOs:

- User
- Item
- Rental
- Conversation
- Message
- Review
- Notification
- Report

This keeps the React app working while still using relational IDs internally.

---

## 16. Complete API Migration Specification

### 16.1 Health

#### `GET /api/health`

Auth: Public

Response:

```json
{
  "success": true,
  "message": "Rentify API is running!",
  "timestamp": "2026-09-01T10:00:00Z"
}
```

### 16.2 Auth

#### `POST /api/auth/register`

Auth: Public

Request:

```json
{
  "name": "Swayam",
  "email": "swayam@example.com",
  "password": "secret123",
  "campus": "Main Campus"
}
```

Validation:

- `name`: required, 2-50 chars
- `email`: required, valid email
- `password`: required, min 6 chars
- `campus`: optional

Response: `201 Created`

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "user": {},
    "token": "jwt"
  }
}
```

#### `POST /api/auth/login`

Auth: Public

Validation:

- `email`: required, valid email
- `password`: required

Response: `200 OK`

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "user": {},
    "token": "jwt"
  }
}
```

#### `GET /api/auth/me`

Auth: Authenticated

Response:

```json
{
  "success": true,
  "message": "Profile fetched",
  "data": {
    "user": {}
  }
}
```

### 16.3 Users

#### `GET /api/users/{id}`

Auth: Public

Response:

```json
{
  "success": true,
  "message": "User fetched",
  "data": {
    "user": {}
  }
}
```

#### `PUT /api/users/profile`

Auth: Authenticated

Allowed body fields:

- `name`
- `bio`
- `campus`
- `phone`
- `avatar`

Response:

```json
{
  "success": true,
  "message": "Profile updated",
  "data": {
    "user": {}
  }
}
```

### 16.4 Items

#### `GET /api/items`

Auth: Public

Query params:

- `page`: default 1
- `limit`: default 12, max 50
- `search`
- `category`
- `condition`
- `owner`
- `sort`: `price_asc`, `price_desc`, `oldest`, default newest

Behavior:

- Always filter `isAvailable = true`.
- Search title and description case-insensitively.
- Return paginated envelope.

#### `GET /api/items/mine`

Auth: Authenticated

Response:

```json
{
  "success": true,
  "message": "Your items fetched",
  "data": {
    "items": []
  }
}
```

#### `GET /api/items/{id}`

Auth: Public

Response includes owner fields:

- name
- email
- avatar
- campus
- rating
- totalReviews

#### `POST /api/items`

Auth: Authenticated

Validation:

- `title`: 1-100
- `description`: 1-1000
- `category`: enum
- `pricePerDay`: > 0 and <= 100000
- `images`: optional, max 5 URLs
- `condition`: optional enum
- `location`: optional max 200

Response: `201 Created`

#### `PUT /api/items/{id}`

Auth: Authenticated owner only

Allowed fields:

- `title`
- `description`
- `category`
- `pricePerDay`
- `images`
- `condition`
- `location`
- `isAvailable`

Response:

```json
{
  "success": true,
  "message": "Item updated",
  "data": {
    "item": {}
  }
}
```

#### `DELETE /api/items/{id}`

Auth: Authenticated owner only

Behavior:

- Reject if approved or active rentals exist.
- Cancel pending rentals.
- Delete item.

Response:

```json
{
  "success": true,
  "message": "Item deleted"
}
```

### 16.5 Rentals

#### `POST /api/rentals`

Auth: Authenticated

Request:

```json
{
  "itemId": "1",
  "startDate": "2026-09-10",
  "endDate": "2026-09-12",
  "message": "Can I rent this for lab work?",
  "offerPrice": 300
}
```

Validation:

- `itemId`: required
- `startDate`: today or future
- `endDate`: after `startDate`
- `message`: optional max 500
- `offerPrice`: optional positive

Response: `201 Created`

Behavior:

- Reject self-rental.
- Reject overlap with approved/active rental.
- Calculate total price by days between start and end.
- Create rental, conversation, and notification in one transaction.

#### `GET /api/rentals/mine`

Auth: Authenticated renter

Returns outgoing rentals.

#### `GET /api/rentals/received`

Auth: Authenticated owner

Returns incoming rental requests.

#### `GET /api/rentals/{id}`

Auth: Authenticated participant only

Returns fully populated rental.

#### `PATCH /api/rentals/{id}/status`

Auth: Authenticated

Request:

```json
{
  "status": "approved"
}
```

Rules:

- Owner approves/rejects pending requests.
- Either participant can cancel pending/approved/active.
- Owner marks approved rental active.
- Owner marks active rental completed.
- Completed/rejected/cancelled are terminal.

Approval behavior:

- Lock and re-check overlap.
- Approve selected rental.
- Auto-cancel overlapping pending rentals.
- Notify affected users.

### 16.6 Chat

#### `GET /api/chat`

Auth: Authenticated

Returns conversations where current user is a participant, sorted by `lastMessageAt DESC`.

#### `GET /api/chat/unread`

Auth: Authenticated

Response:

```json
{
  "success": true,
  "message": "Unread count fetched",
  "data": {
    "count": 3
  }
}
```

#### `GET /api/chat/{conversationId}`

Auth: Authenticated participant only

Behavior:

- Mark messages from other participant as read.
- Return chronological messages.

#### `POST /api/chat/{conversationId}`

Auth: Authenticated participant only

Request:

```json
{
  "content": "Sounds good, see you tomorrow."
}
```

Validation:

- `content`: required, 1-2000 chars

Behavior:

- Save message.
- Update conversation last message and timestamp.
- Optionally create message notification for other participant.

### 16.7 Reviews

#### `POST /api/reviews`

Auth: Authenticated

Request:

```json
{
  "rentalId": "1",
  "rating": 5,
  "itemRating": 4,
  "comment": "Good experience."
}
```

Rules:

- Rental must be completed.
- Reviewer must be owner or renter.
- One review per rental per reviewer.
- Renter reviewing owner creates `lender` review.
- Owner reviewing renter creates `renter` review.

#### `GET /api/reviews/user/{userId}`

Auth: Public

Returns reviews where user is reviewee.

#### `GET /api/reviews/rental/{rentalId}`

Auth: Public

Returns reviews for rental.

### 16.8 Upload

#### `POST /api/upload`

Auth: Authenticated

Request:

- `multipart/form-data`
- field name: `images`
- up to 5 image files
- max 5 MB each

Response: `201 Created`

```json
{
  "success": true,
  "message": "Images uploaded successfully",
  "data": {
    "imageUrls": []
  }
}
```

Cloudinary:

- Upload as image.
- Use secure URL.
- Resize/limit to max 1000x1000 if supported through Cloudinary transformation.

### 16.9 Notifications

#### `GET /api/notifications`

Auth: Authenticated

Returns latest 50 notifications and unread count.

#### `PATCH /api/notifications/{id}/mark-read`

Auth: Authenticated owner only

Marks current user's notification as read.

#### `PATCH /api/notifications/mark-all-read`

Auth: Authenticated

Marks all current user's notifications as read.

### 16.10 Reports

#### `POST /api/reports`

Auth: Authenticated

Request:

```json
{
  "reportedUserId": "2",
  "rentalId": "4",
  "reason": "Item Damage",
  "description": "The returned item was damaged.",
  "evidenceImage": "https://res.cloudinary.com/..."
}
```

Rules:

- Rental must be completed.
- Reporter must be participant.
- Reported user must be other participant.
- Notify admins.

#### `GET /api/reports/admin`

Auth: Admin

Returns all reports.

#### `PATCH /api/reports/admin/{id}/respond`

Auth: Admin

Request:

```json
{
  "message": "We reviewed your report and took action.",
  "status": "resolved",
  "action": "account_suspended"
}
```

Behavior:

- Update report notes/status/action.
- Apply selected moderation action.
- Notify reporter.

### 16.11 Admin

#### `GET /api/admin/users`

Auth: Admin

Returns all users sorted newest first.

#### `DELETE /api/admin/users/{id}`

Auth: Admin

Behavior:

- Purge user and associated data.
- Delete Cloudinary assets after database commit.

Response:

```json
{
  "success": true,
  "message": "User and all associated data deleted successfully"
}
```

---

## 17. Validation and Error Handling

### 17.1 Validation

Use Jakarta Bean Validation on request DTOs:

- `@NotBlank`
- `@NotNull`
- `@Email`
- `@Size`
- `@Min`
- `@Max`
- `@DecimalMin`
- custom date validation where needed

For rental dates, validate in service as well because cross-field validation is easier and clearer there:

- `startDate` must not be before today.
- `endDate` must be after `startDate`.

### 17.2 Error Statuses

Use these consistently:

| Situation | Status |
|---|---|
| Invalid request body/query | 400 |
| Missing/invalid token | 401 |
| Authenticated but not allowed | 403 |
| Entity not found | 404 |
| Duplicate email or duplicate review | 409 |
| Unexpected error | 500 |

### 17.3 Error Envelope

Production:

```json
{
  "success": false,
  "message": "Item not found"
}
```

Development may include stack trace only if needed, matching current behavior.

### 17.4 Global Exception Handler

Implement `@RestControllerAdvice` that handles:

- custom `ApiException`
- `MethodArgumentNotValidException`
- `ConstraintViolationException`
- `DataIntegrityViolationException`
- JWT exceptions
- generic `Exception`

Keep messages user-friendly because the frontend displays them in toasts.

---

## 18. Search, Filtering, Sorting, and Pagination

### 18.1 Pagination

Frontend uses 1-based page numbers. Spring Data uses 0-based indexes.

Controller conversion:

```text
springPage = max(page, 1) - 1
limit = clamp(limit, 1, 50)
```

Response:

```json
{
  "pagination": {
    "page": 1,
    "limit": 12,
    "total": 45,
    "pages": 4
  }
}
```

### 18.2 Sorting

Map frontend values:

- `price_asc` -> `pricePerDay ASC`
- `price_desc` -> `pricePerDay DESC`
- `oldest` -> `createdAt ASC`
- default/newest -> `createdAt DESC`

### 18.3 Search

Initial implementation:

- `lower(title) like %query%`
- OR `lower(description) like %query%`

Reason:

- Simple.
- No extra infrastructure.
- Easy to explain.
- Good enough for campus marketplace scale.

Optional improvement:

- Add PostgreSQL `pg_trgm` or full-text search after the app is complete.

---

## 19. Cloudinary Migration

### 19.1 Configuration

Environment variables:

```text
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
```

Create `CloudinaryConfig` and `CloudinaryService`.

### 19.2 Upload Rules

- Endpoint field name must remain `images`.
- Accept up to 5 files.
- Reject files larger than 5 MB.
- Accept image MIME types only.
- Return secure URLs.

### 19.3 Deletion Rules

Implement helper to extract Cloudinary public ID from URL.

Use deletion in:

- item deletion
- admin nuclear user deletion
- optional cleanup when item image is removed during edit

Important:

- Do not let Cloudinary deletion failure roll back successful database transactions.
- Log cleanup failures so they can be retried manually.

---

## 20. Configuration

### 20.1 `application.yml`

```yaml
server:
  port: ${PORT:4000}

spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/rentify}
    username: ${DATABASE_USERNAME:postgres}
    password: ${DATABASE_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
    open-in-view: false
  flyway:
    enabled: true
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 25MB

app:
  jwt:
    secret: ${JWT_SECRET}
    expires-in: ${JWT_EXPIRES_IN:7d}
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}
  cloudinary:
    cloud-name: ${CLOUDINARY_CLOUD_NAME}
    api-key: ${CLOUDINARY_API_KEY}
    api-secret: ${CLOUDINARY_API_SECRET}
```

### 20.2 Environment Variables

Required:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`

Optional:

- `PORT`
- `JWT_EXPIRES_IN`
- `CORS_ALLOWED_ORIGINS`

---

## 21. Seed Data

The current Node project references a missing seed script. The Spring migration should include a simple dev seed option.

Create either:

- `data.sql` for local-only basic data, or
- `DevDataSeeder` activated only under `dev` profile.

Prefer `DevDataSeeder`.

Seed:

- one admin user
- three student users
- several items across categories
- a few rentals in different statuses
- notifications/reviews if helpful for UI testing

Never run seed logic in production profile.

---

## 22. Testing Plan

Minimum tests for interview confidence:

### 22.1 Unit Tests

- `AuthService`
  - duplicate email
  - password hashing/login
  - suspended user blocked
- `RentalService`
  - self-rental rejected
  - invalid date rejected
  - overlap rejected
  - allowed status transitions
  - disallowed status transitions
- `ReviewService`
  - completed rental required
  - duplicate review rejected
  - rating aggregates update correctly

### 22.2 Integration Tests

Use Testcontainers PostgreSQL.

Cover:

- Register/login/me flow.
- Item create/list/detail flow.
- Rental create/approve/auto-cancel flow.
- Chat send/read/unread flow.
- Review create and fetch flow.
- Admin report response and user suspension.

### 22.3 Manual Frontend Regression

With React connected to Spring backend:

- Register and login.
- Create listing with images.
- Search/filter/sort item list.
- Create rental request.
- Approve rental as owner.
- Verify overlapping pending rental is cancelled.
- Mark active and completed.
- Submit both renter and owner reviews.
- Send chat messages.
- Check unread notifications.
- File dispute report.
- Resolve report as admin.
- Delete user as admin.

---

## 23. Phased Antigravity Implementation Plan

### Phase 1: Spring Boot Skeleton

Deliver:

- New Spring Boot backend project.
- Dependencies installed.
- PostgreSQL connection.
- Flyway configured.
- Health endpoint.
- Response envelope classes.
- Global exception handler.

Acceptance:

- App starts on port 4000.
- `GET /api/health` works.
- Flyway creates schema.

### Phase 2: Entities, Repositories, and Migrations

Deliver:

- All 8 domain entities.
- Supporting `item_images` and `conversation_participants`.
- Enums with frontend-compatible JSON values.
- Repositories and key queries.

Acceptance:

- App starts with `ddl-auto=validate`.
- Repository tests can save and load core relationships.

### Phase 3: Auth and Security

Deliver:

- Registration.
- Login.
- JWT generation/validation.
- Current user endpoint.
- Security config.
- Suspended-user blocking.
- Admin authorization.

Acceptance:

- React login/register works.
- Token authenticates protected endpoints.
- Non-admin receives 403 on admin routes.

### Phase 4: User and Item APIs

Deliver:

- User public profile.
- Profile update.
- Item list/search/filter/sort/pagination.
- My items.
- Item detail.
- Create/update/delete item.
- Image URL support.

Acceptance:

- Homepage marketplace works from React.
- Create/edit listing works.
- Delete rules match old backend.

### Phase 5: Upload and Cloudinary

Deliver:

- Multipart upload endpoint.
- Cloudinary service.
- File count/type/size validation.
- URL response compatibility.

Acceptance:

- React image upload works.
- Returned URLs can be saved into item images.

### Phase 6: Rentals and Conversations

Deliver:

- Create rental.
- Outgoing/incoming rentals.
- Rental detail.
- Status transitions.
- Overlap detection.
- Approval locking.
- Auto-cancel conflicting pending requests.
- Conversation creation.
- Rental notifications.

Acceptance:

- Full rental request workflow works from React.
- Double-booking is prevented.
- Conversation exists after request creation.

### Phase 7: Chat and Notifications

Deliver:

- Conversation list.
- Message list.
- Send message.
- Read receipts.
- Unread count.
- Notification list.
- Mark read.
- Mark all read.

Acceptance:

- Chat page works.
- Notification polling works with the existing frontend.

### Phase 8: Reviews and Ratings

Deliver:

- Create review.
- User reviews.
- Rental reviews.
- User rating aggregate updates.
- Item rating updates.

Acceptance:

- Completed rental allows both participant reviews.
- Duplicate review is rejected.
- Profile and item ratings update correctly.

### Phase 9: Reports and Admin

Deliver:

- Submit report.
- Admin report list.
- Admin respond/action.
- Admin user list.
- Nuclear user deletion.
- Cloudinary cleanup after commit.

Acceptance:

- Admin dashboard works.
- Suspension blocks the user on next request.
- User deletion removes associated records without DB errors.

### Phase 10: Hardening and Interview Polish

Deliver:

- Integration tests for critical flows.
- Dev seed profile.
- README setup instructions.
- API documentation or endpoint summary.
- Cleanup of any frontend compatibility issues.

Acceptance:

- Project can be cloned, configured, seeded, and demoed.
- Core flows are stable enough for interview demonstration.

---

## 24. Implementation Rules for Antigravity

- Do not redesign the product.
- Do not change the React frontend unless compatibility requires a tiny adjustment.
- Prefer backend DTO compatibility over frontend rewrites.
- Do not expose JPA entities directly.
- Do not put business logic in controllers.
- Do not skip transactions for rental/review/admin flows.
- Do not add WebSockets, payment gateways, Redis, queues, or microservices.
- Do not replace Cloudinary unless explicitly instructed.
- Do not use floating-point types for money; use `BigDecimal`.
- Do not use `Date` or `Instant` for rental date ranges; use `LocalDate`.
- Do not trust frontend role/user IDs; derive current user from JWT.
- Do not let users update protected fields through profile or item APIs.
- Keep error messages clear and frontend-displayable.
- Keep code conventional enough to explain in an interview.

---

## 25. Definition of Done

The migration is complete when:

- Spring Boot backend starts cleanly.
- PostgreSQL schema is managed by Flyway.
- All 8 domain entities are implemented.
- Existing 24 API endpoints are implemented or intentionally documented if changed.
- React frontend can authenticate against Spring backend.
- React frontend can browse/search/filter/sort items.
- Users can create/edit/delete listings.
- Upload endpoint returns Cloudinary URLs.
- Rental request lifecycle works end to end.
- Overlapping approved/active rentals are prevented.
- Conflicting pending rentals are auto-cancelled on approval.
- Chat conversations/messages work.
- Notification list/unread/mark-read flows work.
- Reviews can be submitted only after completion.
- User and item rating aggregates update correctly.
- Reports can be filed for completed rentals.
- Admin report actions work.
- Admin user deletion cleans associated data.
- Suspended users are blocked on the next authenticated request.
- Error envelopes match the existing frontend expectation.
- At least core service/integration tests exist for auth, rental overlap, status transitions, reviews, and admin deletion.
- README explains local setup, environment variables, seed data, and demo flow.

---

## 26. Interview Talking Points

### Why Spring Boot Monolith?

Rentify is one cohesive application with one database and tightly connected workflows. A monolith is simpler to build, deploy, debug, and explain. Microservices would add operational complexity without solving a real problem at this scale.

### Why PostgreSQL?

The domain is relational:

- users own items
- rentals connect users and items
- conversations belong to rentals
- reviews belong to rentals and users
- reports reference users and rentals

PostgreSQL gives strong foreign keys, transactions, constraints, indexes, and reliable consistency for rental workflows.

### Why JPA/Hibernate?

JPA maps the object model cleanly to relational tables, reduces repetitive SQL for normal CRUD, and still allows custom JPQL/native queries for overlap checks and bulk updates.

### How Was MongoDB Mapping Handled?

Mongo document references became foreign keys. Arrays such as item images and conversation participants became separate relational tables. Mongoose populate was replaced with JPA relationships and DTO mapping.

### How Is Double Booking Prevented?

The service checks for overlapping approved/active rentals using:

```text
requestedStart <= existingEnd AND requestedEnd >= existingStart
```

During approval, the relevant rental rows are locked inside a transaction, conflicts are rechecked, and overlapping pending requests are cancelled.

### Why Use LocalDate for Rentals?

Rentals are calendar-day based, not exact timestamp based. `LocalDate` avoids timezone bugs where a date can shift near midnight between client and server.

### How Are Ratings Updated?

Ratings use rolling averages:

```text
newAverage = ((oldAverage * oldCount) + newRating) / (oldCount + 1)
```

The update runs inside the same transaction as review creation so the review and aggregate stay consistent.

### How Is Security Implemented?

Spring Security uses a custom JWT filter. The filter validates the token, loads the user, checks suspension status, and creates the authenticated principal. Admin endpoints require the `admin` role.

### Why Keep Polling Instead of WebSockets?

The existing frontend already polls notifications. Keeping polling reduces migration risk and avoids adding real-time infrastructure before the core migration is stable. WebSockets can be a future enhancement.

### How Is Cloudinary Handled Safely?

Uploads happen through a dedicated service. Deletions are performed after successful database commits where possible, so the database does not roll back after remote files were already deleted.

### What Would Be Improved Later?

Future improvements could include:

- PostgreSQL full-text search or trigram indexes.
- WebSocket/SSE for chat and notifications.
- Payment integration.
- Email verification.
- Password reset.
- Stronger database-level exclusion constraint for rental overlaps.
- Token revocation for immediate logout/password-change invalidation.

---

## 27. Final Recommendation

Implement this migration in phases. Do not ask Antigravity to rewrite everything in one pass. The safest path is:

1. Build the Spring Boot foundation.
2. Implement the database model.
3. Make auth work.
4. Make item browsing/listing work.
5. Make rentals and state transitions work.
6. Add chat, notifications, reviews, reports, and admin cleanup.
7. Connect the React frontend and fix only compatibility issues.

This produces a clean, interview-defensible project without unnecessary architecture.
