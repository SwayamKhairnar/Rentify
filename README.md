# 📦 Rentify — Campus Equipment Sharing Platform

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![React](https://img.shields.io/badge/React-18-61DAFB.svg)](https://reactjs.org/)
[![Swagger](https://img.shields.io/badge/OpenAPI-3.0-85EA2D.svg)](http://localhost:4000/swagger-ui.html)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](https://www.docker.com/)

**Rentify** is a production-grade, peer-to-peer campus equipment rental platform that enables university students and staff to list, discover, rent, review, and message each other securely.

Built as a high-performance **Spring Boot 3.3** monolithic backend with a responsive **React 18** frontend, backed by **PostgreSQL 16** with **Flyway** schema versioning, stateless **JWT authentication**, **WebSocket (STOMP)** real-time messaging, and **Cloudinary** media storage.

---

## 🏛️ System Architecture

```mermaid
graph TD
    User["Web Browser (React 18 / Tailwind)"] -->|HTTP / REST| Nginx["Nginx Reverse Proxy (:80)"]
    User -->|WebSocket (STOMP)| Nginx
    
    Nginx -->|/api/*| SpringBoot["Spring Boot Monolith (:4000)"]
    Nginx -->|/ws/*| SpringBoot
    Nginx -->|/*| Static["React SPA Static Files"]
    
    subgraph Spring Boot Layer
        Security["Spring Security (HMAC-SHA512 JWT + RBAC)"]
        Controllers["REST & WebSocket Controllers"]
        Services["Domain Services & Atomic Rating Engine"]
        Specifications["Dynamic JPA Specifications"]
        Repositories["Spring Data JPA Repositories"]
    end
    
    SpringBoot --> Security
    Security --> Controllers
    Controllers --> Services
    Services --> Specifications
    Services --> Repositories
    
    Repositories --> PostgreSQL[("PostgreSQL 16 (Flyway Migrations)")]
    Services --> Cloudinary[("Cloudinary Media Cloud")]
```

---

## ✨ Key Features

- **🔐 Robust Security & Auth:** Stateless HMAC-SHA512 JWT authentication, BCrypt password hashing, method-level RBAC (`@PreAuthorize("hasRole('ADMIN')")`), and instant token blocking on account suspension.
- **📅 Double-Booking Prevention:** Strict 5-stage rental lifecycle (`pending` → `approved`/`rejected` → `active` → `completed`/`cancelled`) with inclusive date calculation and date-overlap locking.
- **⭐ Atomic 4-Tier Rating Engine:** Weighted reputation calculation for lenders, renters, platform overall, and equipment quality with 1 decimal precision.
- **💬 Real-Time Messaging & Notifications:** WebSockets with STOMP over SockJS (`/topic/conversations/{id}`), unread badge counts, and asynchronous event notifications.
- **🛡️ Admin & Dispute Suite:** System statistics dashboard, user suspension toggle, equipment moderation, and dispute resolution workflows.
- **📄 Interactive API Documentation:** Live OpenAPI 3.0 / Swagger UI dashboard at `/swagger-ui.html` with built-in JWT authorization.
- **🐳 Single-Command Docker Deployment:** Multi-stage Dockerfiles with healthchecked Docker Compose.

---

## 🚀 Quick Start with Docker Compose (Recommended)

Run the entire application (PostgreSQL + Spring Boot + React) with zero local dependencies:

```bash
# Clone the repository
git clone https://github.com/your-username/Rentify.git
cd Rentify

# Start all services
docker compose up --build
```

- **Frontend App:** `http://localhost:3000` (or `http://localhost:80`)
- **Backend API:** `http://localhost:4000/api`
- **Interactive Swagger UI:** `http://localhost:4000/swagger-ui.html`

---

## 💻 Local Development Setup

### 1. Backend (Spring Boot 3.3 + Java 21)
```bash
# Ensure PostgreSQL is running (database: rentify)
cd backend
mvn clean test             # Run 68+ integration tests
mvn spring-boot:run        # Starts on port 4000
```

### 2. Frontend (React 18 + Vite)
```bash
cd frontend
npm install
npm run dev                # Starts on http://localhost:5173
```

---

## 👥 Seed Demo Accounts

The database seeds automatically on first run with demo data:

| Email | Password | Role | Bio / Gear |
| :--- | :--- | :--- | :--- |
| `admin@example.com` | `password123` | `ADMIN` | Platform Administrator |
| `john@example.com` | `password123` | `STUDENT` | Sony Alpha Camera, Yamaha Guitar |
| `jane@example.com` | `password123` | `STUDENT` | TI-84 Calculator, Chemistry Textbook |
| `sarah@example.com` | `password123` | `STUDENT` | Trek Road Bike |

---

## 🧪 Testing & Verification

```bash
cd backend
mvn clean test
```
All **68+ automated tests** execute against an isolated database covering:
- Authentication & JWT token lifecycles
- Booking state machine & overlap validations
- Peer-to-peer rating recalculation
- Dynamic specification filtering
- Admin privilege guards & dispute resolutions
