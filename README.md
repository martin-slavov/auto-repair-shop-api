# Auto Repair Shop API

A REST API for managing an auto repair shop — vehicles, service requests, mechanic assignments, parts, and invoicing. Built with Spring Boot, Spring Security (JWT), and JPA/Hibernate.

## Tech Stack

- **Java 21**, **Spring Boot 4.1.1**, **Spring Framework 7**
- **Spring Security 7.1.1** with custom JWT authentication
- **Spring Data JPA** / **Hibernate 7.4.5**
- **MySQL** with **Flyway** migrations
- **Bean Validation** with custom validators
- **Spring Mail** (Mailtrap sandbox) for async email notifications
- **springdoc-openapi** (Swagger UI) for API documentation

## Features

- Role-based access control (ADMIN, MECHANIC, CUSTOMER) with JWT authentication
- Ownership-based authorization (customers only see their own vehicles/requests/invoices)
- Full service request workflow: submit → approve/reject → auto-create visit → assign mechanic
- Visit lifecycle management: SCHEDULED → IN_PROGRESS → COMPLETED, with parts and labor tracking
- Parts catalog with role-aware pricing (mechanics don't see internal cost/margin)
- Automatic invoice generation from parts used and hours worked
- Async email notifications to admin on new service requests
- Global exception handling with consistent JSON error responses
- Interactive API documentation via Swagger UI

## Getting Started

### Prerequisites
- Java 21
- MySQL 8.0+
- Maven

### Setup

1. Clone the repository:
```bash
   git clone https://github.com/martin-slavov/auto-repair-shop-api.git
   cd auto-repair-shop-api
```

2. Create the database:
```sql
   CREATE DATABASE auto_repair_shop;
```

3. Copy `application.properties.example` to `application.properties`, and fill in your own values:
```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
```
You'll need to set: database credentials, a JWT secret (generate with `openssl rand -base64 32`), and Mailtrap SMTP credentials (free sandbox account at mailtrap.io).

4. Run the application (Flyway migrations apply automatically on startup):
```bash
   ./mvnw spring-boot:run
```

5. Open Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Demo Credentials

An admin account is seeded automatically on first run:
- **Username:** `admin`
- **Password:** `admin@123`

Register a new customer via `POST /api/auth/register`, or have the admin create a mechanic via `POST /api/admin/users/mechanic`.

## API Overview

Authenticate via `POST /api/auth/login` to receive a JWT token, then include it as `Authorization: Bearer <token>` on subsequent requests. Full interactive documentation is available via Swagger UI (see above).

| Method | Endpoint | Description | Role |
|---|---|---|---|
| POST | `/api/auth/register` | Register a new customer | Public |
| POST | `/api/auth/login` | Log in, receive JWT | Public |
| POST | `/api/admin/users/mechanic` | Create a mechanic account | Admin |
| GET / POST | `/api/vehicles` | List / create vehicles | Customer, Admin |
| GET | `/api/vehicles/{id}` | Get a specific vehicle | Owner, Admin |
| GET / POST | `/api/service-requests` | List / submit service requests | Customer, Admin |
| PATCH | `/api/service-requests/{id}/approve` | Approve request, assign mechanic | Admin |
| PATCH | `/api/service-requests/{id}/reject` | Reject request | Admin |
| GET | `/api/service-visits` | List visits for current mechanic | Mechanic |
| PATCH | `/api/service-visits/{id}/status` | Update visit status | Mechanic |
| PATCH | `/api/service-visits/{id}/complete` | Complete visit with notes/hours | Mechanic |
| PATCH | `/api/service-visits/{id}/invoice` | Generate invoice for completed visit | Admin |
| GET / POST | `/api/parts` | List / add catalog parts | Admin, Mechanic (GET) |
| GET / POST | `/api/service-visits/{id}/parts` | List / add parts used in a visit | Mechanic |
| GET | `/api/invoices` | List invoices | Customer (own), Admin (all) |
| PATCH | `/api/invoices/{id}/pay` | Mark invoice as paid | Admin |

## Architecture Notes

- **DTO pattern** throughout — controllers never expose entities directly.
- **Custom exceptions** (`ResourceNotFoundException`, `DuplicateResourceException`, `OwnershipViolationException`, `InvalidStateTransitionException`) mapped to correct HTTP statuses via a global `@RestControllerAdvice`.
- **Mapper classes** separate entity-to-DTO conversion from business logic.
- **Ownership checks** are enforced in the service layer, not just at the security-rule level.