# 🎯 Rentify — Technical Interview Guide & Talking Points

This guide gives you the exact architectural reasoning, deep-dive answers, and demo script to present **Rentify** during software engineering technical interviews.

---

## 1. The 60-Second Elevator Pitch

> *"Rentify is a peer-to-peer campus equipment rental platform I built with Spring Boot 3.3, Java 21, React 18, and PostgreSQL. It solves the problem of high upfront costs for university equipment by letting students securely lend and rent cameras, bikes, and textbooks.*
> 
> *Architecturally, I designed it as a modular monolith with strict transactional consistency. It features a 5-stage rental lifecycle state machine with date-overlap locking to prevent double bookings, an atomic 4-tier rating engine for peer reputation, real-time messaging via WebSockets (STOMP), stateless JWT authentication with method-level RBAC, and containerized deployment via Docker Compose."*

---

## 2. Key Architectural Decisions (Why You Built It This Way)

### Q: Why a Modular Monolith instead of Microservices?
- **Answer:** *"For a peer-to-peer rental marketplace of this scale, a well-structured modular monolith is superior to microservices. It guarantees ACID transactions across bookings, payments, and ratings without distributed transaction overhead (like 2PC or Saga patterns), avoids network latency between services, simplifies deployments, and keeps operational costs minimal. Each domain (`auth`, `item`, `rental`, `review`, `conversation`, `admin`) has clear boundary layers that could be split into independent services later if traffic necessitates."*

### Q: How did you prevent race conditions & double-booking?
- **Answer:** *"I implemented database-level date-range overlap validation using Spring Data JPA custom queries. Before creating or approving a booking, we check for overlapping date intervals (`startDate <= :newEnd AND endDate >= :newStart`) against all non-terminal bookings (`APPROVED` and `ACTIVE`). Coupled with `@Version` optimistic locking on entities and transaction isolation, concurrent booking requests for the same dates fail fast with structured 400 Bad Request responses."*

### Q: How does the Atomic Rating Aggregation Engine work?
- **Answer:** *"In a two-sided marketplace, peer trust is multi-dimensional. When a rental is completed, both parties can leave reviews. I built a dedicated `RatingAggregationService` that atomically calculates 4 distinct metrics: `lenderRating` (how good the user is as an owner), `renterRating` (how reliable they are as a borrower), `itemQualityAverage` (average condition of their items), and platform `rating`. Calculations run inside the transaction and round to 1 decimal place."*

### Q: How did you handle Security & Instant Suspension?
- **Answer:** *"We use stateless HMAC-SHA512 JWTs. To solve the classic JWT revocation problem where a banned user retains access until token expiration, the `JwtAuthenticationFilter` performs a lightweight database check on the user's `isSuspended` flag. If an admin bans a user, their subsequent API requests are rejected immediately with `403 Forbidden`."*

---

## 3. 5-Minute Live Demo Script for Interviewers

1. **Step 1: The UI Experience (`http://localhost:5173` or `:3000`)**
   - Log in as `john@example.com` (`password123`).
   - Browse catalog items (cameras, road bikes) and filter by category or condition.
   - Show the Booking Modal with price calculation.
   - Switch to Dashboard: show "My Listings", "My Rentals" (borrowed), and "Received Requests" (lent) with state transition buttons (Approve, Start, Complete).

2. **Step 2: Real-Time Chat & Notifications**
   - Open a rental booking conversation.
   - Show how messages link directly to the equipment and auto-clear unread badge counts upon viewing.

3. **Step 3: Interactive Swagger UI (`http://localhost:4000/swagger-ui.html`)**
   - Open the Swagger UI page in the browser.
   - Log in via `/api/auth/login` to obtain a JWT.
   - Click **Authorize 🔓** and paste the token.
   - Execute `/api/admin/stats` to show live aggregated platform statistics.

4. **Step 4: Code & Tests (`mvn clean test`)**
   - Run `mvn clean test` in terminal to show **68+ passing integration tests**.
   - Point out Flyway migration `V1__initial_schema.sql` and the clean package structure.
