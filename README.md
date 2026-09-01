# Rentify

[![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![React](https://img.shields.io/badge/React-18-61DAFB.svg?style=flat-square&logo=react)](https://reactjs.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?style=flat-square&logo=docker)](https://www.docker.com/)
[![Tests](https://img.shields.io/badge/Tests-70%20Passing-success.svg?style=flat-square)]()
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

A peer-to-peer equipment rental marketplace designed for campus communities. Rentify allows students and faculty to list, discover, rent, and communicate in real time, reducing the cost of short-term equipment access.

---

## 🔗 Live Links

- **Web Application:** [https://rentify-liart-eta.vercel.app](https://rentify-liart-eta.vercel.app)
- **API Documentation & Swagger UI:** [https://rentify-xam8.onrender.com/swagger-ui.html](https://rentify-xam8.onrender.com/swagger-ui.html)
- **Health Endpoint:** [https://rentify-xam8.onrender.com/api/health](https://rentify-xam8.onrender.com/api/health)

---

## Key Features

- **Equipment Catalog & Search:** Filter by category (textbooks, cameras, electronics, bikes, sports, instruments) with price sorting and keyword search.
- **Conflict-Free Booking:** Transactional date-interval validation prevents double-booking race conditions across concurrent requests.
- **Real-Time Chat:** Bidirectional messaging using Spring WebSockets with the STOMP protocol and SockJS fallback.
- **4-Tier Peer Reviews:** Atomic rating recalculation for renters, owners, and individual items.
- **Admin Moderation:** Role-based dashboard (`ROLE_ADMIN`) with dispute resolution, report handling, and user suspension.
- **Image Pipeline:** Cloudinary CDN integration with automatic image resizing, format optimization, and secure delivery.
- **Dark / Light Mode:** Obsidian dark theme and high-contrast porcelain light theme with CSS variable transitions.

---

## Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend** | Spring Boot 3.3.4, Java 21, Spring Data JPA, Spring Security 6.3, Spring WebSocket (STOMP) |
| **Database** | PostgreSQL 16 (Neon.tech), Flyway Migration Tool |
| **Frontend** | React 18, Vite, React Router 6, Lucide Icons |
| **Authentication** | Stateless JWT (HMAC-SHA512), BCrypt Password Hashing |
| **Cloud & Media** | Cloudinary Image CDN, Java Mail (SMTP) |
| **DevOps & Hosting** | Docker, Docker Compose, Nginx, Render, Vercel |
| **Testing** | JUnit 5, AssertJ, MockMvc (70 automated integration tests) |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    React 18 Single-Page App                 │
│              (Vercel Edge Network / Nginx Container)        │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTPS / WSS
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 Spring Boot 3.3 REST + STOMP API            │
│                     (Java 21 / Docker Container)            │
│                                                             │
│   [Auth / JWT]    [Item Catalog]    [Rental Engine]         │
│   [WebSocket]     [Review Engine]   [Admin Dashboard]       │
└──────────────────────────────┬──────────────────────────────┘
                               │ HikariCP (JDBC)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 PostgreSQL 16 Relational DB                 │
│                 (Flyway Managed Migrations)                 │
└─────────────────────────────────────────────────────────────┘
```

---

## Quickstart (Local Development)

### Prerequisites
- [Docker & Docker Compose](https://docs.docker.com/get-docker/) **or**
- Java 21 (JDK) & Node.js 18+

### 1. Clone the Repository
```bash
git clone https://github.com/SwayamKhairnar/Rentify.git
cd Rentify
```

### 2. Configure Environment Variables
```bash
cp .env.example .env
```
*(Optionally add your Cloudinary keys to `.env` for custom image uploads).*

### 3. Run with Docker Compose
```bash
docker compose up -d --build
```

### 4. Access the Application
- **Frontend App:** [http://localhost:3000](http://localhost:3000)
- **Backend API:** [http://localhost:4000/api/health](http://localhost:4000/api/health)
- **Swagger Documentation:** [http://localhost:4000/swagger-ui.html](http://localhost:4000/swagger-ui.html)

---

## Demo Accounts

The database seeds automatically on startup with the following test credentials:

| Role | Email | Password | Description |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin@example.com` | `password123` | Full admin dashboard & dispute access |
| **User (Owner)** | `john@example.com` | `password123` | Listed camera and textbook |
| **User (Renter)** | `jane@example.com` | `password123` | Active rental requests & reviews |
| **User** | `mike@example.com` | `password123` | Student borrower account |

---

## API Summary

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Public | Create student account |
| `POST` | `/api/auth/login` | Public | Authenticate and obtain JWT |
| `GET` | `/api/items` | Public | Search items with pagination & filters |
| `POST` | `/api/items` | Authenticated | List a new item with image URLs |
| `POST` | `/api/rentals` | Authenticated | Create rental booking request |
| `PATCH`| `/api/rentals/{id}/status` | Authenticated | Approve, reject, or complete rental |
| `GET` | `/api/chat/conversations` | Authenticated | Fetch active chat threads |
| `POST` | `/api/reviews` | Authenticated | Submit rental review & rating |
| `GET` | `/api/admin/stats` | Admin Only | View system-wide platform metrics |
| `PATCH`| `/api/admin/users/{id}/status` | Admin Only | Suspend or reactivate user |

---

## Testing

The test suite covers controllers, services, security filters, validation, and database constraints.

```bash
cd backend
mvn test
```

```
Results:
Tests run: 70, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## License

This project is open source and available under the [MIT License](LICENSE).
