# Rentify - Spring Boot Monolith Backend

This directory houses the modern, high-performance Spring Boot monolithic backend for **Rentify**, replacing the legacy Node.js/Express service with 100% API contract parity, PostgreSQL persistence with Flyway migrations, Spring Security with stateless JWT, Cloudinary media storage, and comprehensive domain validations.

---

## 1. Tech Stack & Architecture

- **Language & Runtime:** Java 21+ / Java 25 (OpenJDK)
- **Framework:** Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Spring Security, Validation)
- **Database:** PostgreSQL with Flyway schema versioning (`V1__initial_schema.sql`)
- **Security:** Stateless JWT authentication (`HMAC-SHA512`), BCrypt password hashing, suspension interceptors, role-based method security (`@PreAuthorize("hasRole('ADMIN')")`)
- **Media Storage:** Cloudinary SDK with secure URL and public ID parsing
- **Testing:** JUnit 5, MockMvc, AssertJ, Spring Boot Test (68+ automated tests)
- **Architecture:** Monolithic layered architecture (`com.rentify.{auth, user, item, rental, review, conversation, notification, report, admin, upload, common, exception}`)

---

## 2. Prerequisites

- **Java Development Kit (JDK):** JDK 21 or newer (Java 25 compatible)
- **Database:** PostgreSQL 14+ running locally (default: `localhost:5432/rentify`)
- **Build Tool:** Apache Maven 3.8+ (or bundled `mvnw`)
- **Frontend (Optional):** Node.js 18+ for running Vite React frontend

---

## 3. Environment Configuration

The backend reads settings from `backend/src/main/resources/application.yml` and environment variables.

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `SERVER_PORT` | `4000` | HTTP port (matches frontend proxy `/api` -> `4000`) |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `rentify` | Database name |
| `DB_USER` | `postgres` | Database username |
| `DB_PASS` | `postgres` | Database password |
| `JWT_SECRET` | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` | 512-bit HMAC secret key |
| `JWT_EXPIRATION_DAYS` | `7` | JWT token validity |
| `CLOUDINARY_CLOUD_NAME` | `rentify-cloud` | Cloudinary cloud identifier |
| `CLOUDINARY_API_KEY` | `demo-api-key` | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | `demo-api-secret` | Cloudinary API secret |

---

## 4. Quick Start: Running the Backend

### Step 1: Initialize Database
```bash
# Ensure PostgreSQL is running and database exists
psql -U postgres -c "CREATE DATABASE rentify;"
```

### Step 2: Build & Start Spring Boot
```bash
cd backend
mvn spring-boot:run
```
Flyway will automatically execute `V1__initial_schema.sql`, and `DataSeeder` will automatically seed demo users, items, rentals, and reviews on first boot!

### Step 3: Run the React Frontend
In a separate terminal:
```bash
cd frontend
npm install
npm run dev
```
The React frontend starts at `http://localhost:5173` and transparently routes all `/api` requests to `http://localhost:4000`.

---

## 5. Seed Demo Accounts

All seed accounts are created with password: `password123`

| Email | Role | Description |
| :--- | :--- | :--- |
| `admin@example.com` | `admin` | Full admin privileges (reports, user suspension, moderation) |
| `john@example.com` | `student` | Lender with Sony Alpha Camera and Acoustic Guitar |
| `jane@example.com` | `student` | Student with TI-84 Calculator and Organic Chemistry book |
| `sarah@example.com` | `student` | Student with Trek Road Bike |

---

## 6. Running Automated Tests

Run the full suite of integration and unit tests:
```bash
cd backend
mvn clean test
```
All tests use Spring MockMvc with transactional isolation (`@Transactional`) against the PostgreSQL database.

---

## 7. Migration Verification Summary

- **Phase 1:** Skeleton, PostgreSQL, Flyway Schema, Health & Standard Error Handling
- **Phase 2:** Domain Entities, Enums, and JPA Repositories
- **Phase 3:** Spring Security, Stateless JWT, BCrypt, Auth Controller (`register`, `login`, `me`)
- **Phase 4:** User Profile Management & Item Catalog with Dynamic JPA Specifications
- **Phase 5:** Cloudinary Multipart Media Upload and Regex Public ID Deletion
- **Phase 6:** Booking Engine, Inclusive Price Calculations, Overlap Locking, Rental Lifecycle State Machine
- **Phase 7:** Dual Peer Reviews, Reviewer Roles, and Atomic Rating Aggregation Engine
- **Phase 8:** 1-on-1 Chat Messaging, Auto-Read Triggers, Unread Badges & Notification Lifecycle
- **Phase 9:** User Dispute Reporting, Admin Dashboard Analytics, User Suspension, and Report Resolution
- **Phase 10:** Idempotent Data Seeding, React Frontend Parity Verification & Full Cutover
