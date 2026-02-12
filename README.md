# uoc-reservations-jdbc

Educational Java backend project built for **UOC – Introduction to Databases**.

This repository implements a clean, framework-free JDBC backend for a restaurant reservation system. The goal is to deeply understand how Java applications interact with relational databases without using Spring, JPA, or any ORM framework.

The project now includes a thin HTTP API layer built with Javalin, exposing backend functionality via REST-style endpoints while preserving strict layered architecture.

---

## Project Status

**Milestone 4 complete – Thin HTTP API**

This version includes:

- Transaction-safe service layer
- Overlap-based availability engine
- Capacity validation
- Reservation lifecycle transitions
- Manual and automatic table assignment
- Active/inactive table enforcement
- Clean CLI interface
- Thin HTTP API layer
- Consistent JSON error model
- ISO-8601 OffsetDateTime serialization
- Manual API smoke tests (`docs/api.http`)
- Full JavaDoc documentation

Tagged milestone: `v0.4.0`

---

# Architecture

The project follows a strict layered architecture:

- **CLI Layer (`edu.uoc.cli`)**  
  Handles user interaction and workflow orchestration.

- **HTTP Layer (`edu.uoc.api`)**  
  Thin REST interface using Javalin.  
  No business logic or SQL inside handlers.

- **Service Layer (`edu.uoc.service`)**  
  Owns business rules and transaction boundaries.  
  All multi-step operations are atomic.

- **DAO Layer (`edu.uoc.dao`)**  
  Pure JDBC persistence logic (SQL + mapping).  
  DAOs never commit or rollback.

- **Domain Model (`edu.uoc.model`)**  
  Core entities: `Customer`, `Reservation`, `Table`.

- **DTO Layer (`edu.uoc.dto`)**  
  Read-optimized projections for query results.

- **Database (`edu.uoc.db`)**  
  Environment-based connection factory.

This mirrors real-world backend design and enforces clear separation of concerns.

---

# HTTP API (Milestone 4)

The project exposes a thin HTTP layer using **Javalin**.

Business rules remain exclusively in the service layer.  
The API only handles:

- Request parsing
- Delegation to service/DAO
- JSON response formatting

---

## Run the API

From terminal:

    ./gradlew runApi

From IntelliJ:
Run `ApiServer`

Server starts at:

    http://localhost:7070

---

## API Endpoints

### Health

    GET /health

---

### Customers

    GET /customers
    GET /customers/{id}

---

### Tables

    GET /tables
    GET /tables/available?start=...&end=...

- `start` is required
- `end` optional
- Must be ISO-8601 OffsetDateTime
- `+` must be URL-encoded as `%2B` if needed

---

### Reservations

    GET    /reservations
    POST   /reservations
    POST   /reservations/{id}/confirm
    POST   /reservations/{id}/cancel

Reservation creation is transaction-safe and assigns tables atomically.

---

## API Conventions

- All responses are JSON
- OffsetDateTime values use ISO-8601 format
- Error responses are consistent:

Example:

    {
      "code": "ERROR_CODE",
      "message": "Human readable message",
      "path": "/request/path"
    }

Status codes:

- 200 → success
- 201 → created
- 400 → invalid input
- 404 → resource not found
- 409 → business conflict
- 500 → unexpected error

---

## Manual API Smoke Tests

The file:

    docs/api.http

Contains runnable API requests compatible with IntelliJ HTTP Client.

This allows quick manual verification of:

- Health
- Availability
- Reservation creation
- Confirm / cancel transitions
- Error handling

---

# Data Model

PostgreSQL schema:

- `customers`
- `tables`
- `reservations`
- `reservation_tables` (many-to-many)

Includes:

- Primary and foreign key constraints
- `tables.active` flag
- Dynamic availability via time window overlap logic

---

# Service Layer (Business Logic)

- Transaction-safe reservation creation
- Default duration rule  
  If `endAt` is null → `startAt + 2 hours`
- Availability / overlap detection  
  `CANCELLED` and `NO_SHOW` do not block tables
- Capacity validation  
  `SUM(table.capacity) >= partySize`
- Lifecycle transitions:
  - `PENDING → CONFIRMED`
  - `PENDING/CONFIRMED → CANCELLED`
- Idempotent cancellation
- Manual validated table assignment
- Automatic greedy assignment
- Active-only enforcement

---

# Project Structure

    uoc-reservations-jdbc/
    │
    ├─ src/main/java/edu/uoc/
    │  ├─ cli/          # CLI layer
    │  ├─ api/          # HTTP layer (Javalin)
    │  ├─ service/      # Business logic
    │  ├─ dao/          # JDBC persistence
    │  ├─ model/        # Domain entities
    │  ├─ dto/          # Query projections
    │  ├─ db/           # Database connection
    │  └─ Main.java     # CLI entry point
    │
    └─ docs/
       ├─ diagrams/
       └─ api.http

---

# Requirements

- Java JDK 17+
- PostgreSQL
- Gradle Wrapper (included)

---

# Database Setup

1. Create database:

   uoc_databases

2. Run schema script:

   01_schema/create_tables.sql

Notes:

- `tables.active` controls structural availability
- Reservation availability is computed dynamically

---

# Configuration (Environment Variables)

Required:

- DB_URL
- DB_USER
- DB_PASSWORD

Example:

    DB_URL=jdbc:postgresql://localhost:5432/uoc_databases
    DB_USER=postgres
    DB_PASSWORD=secret

---

# CLI Mode

Run the CLI version:

    ./gradlew run

CLI remains available for manual interaction and admin configuration.

---

# Transaction Model

- Transactions are owned exclusively by the service layer
- DAOs never commit or rollback
- Multi-step operations are atomic
- Failures trigger rollback automatically

This mirrors production backend architecture.

---

# Learning Goals

This project intentionally avoids frameworks to focus on:

- Relational modeling
- JDBC fundamentals
- Transaction boundaries
- Business rule enforcement
- Clean layered architecture
- Thin HTTP layer design

It serves as:

- A structured academic deliverable
- A backend architectural reference
- A foundation for future REST/web UI integration

---

# License

Educational / personal learning project.
