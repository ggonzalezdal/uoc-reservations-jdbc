# uoc-reservations-jdbc

Educational Java backend project built for **UOC – Introduction to Databases**.

This repository implements a clean, framework-free JDBC backend for a restaurant reservation system. The goal is to deeply understand how Java applications interact with relational databases without using Spring, JPA, or any ORM framework.

The project represents a stable architectural backbone with transaction-safe service logic, enforced business rules, and full JavaDoc documentation.

---

## Architecture

The project follows a strict layered architecture:

- **CLI Layer (`edu.uoc.cli`)**  
  Handles user interaction, input validation, and workflow orchestration.

- **Service Layer (`edu.uoc.service`)**  
  Owns business rules and transaction boundaries. All multi-step operations are atomic.

- **DAO Layer (`edu.uoc.dao`)**  
  Pure JDBC persistence logic (SQL + mapping). DAOs never commit or rollback.

- **Domain Model (`edu.uoc.model`)**  
  Core entities: `Customer`, `Reservation`, `Table`.

- **DTO Layer (`edu.uoc.dto`)**  
  Read-optimized projections for query results.

- **Database (`edu.uoc.db`)**  
  Environment-based connection factory.

This mirrors real-world backend design and enforces clear separation of concerns.

---

## Project Status

Stable architectural baseline complete.

This version includes:

- Transaction-safe service layer
- Overlap-based availability engine
- Capacity validation
- Reservation lifecycle transitions
- Manual and automatic table assignment
- Active/inactive table enforcement
- Refactored CLI package
- Complete JavaDoc documentation

Tagged milestone: `v1.0-backbone`

Further enhancements will build incrementally on top of this foundation.

---

## Implemented Features

### Data Model

PostgreSQL schema:

- `customers`
- `tables` (physical restaurant layout)
- `reservations`
- `reservation_tables` (many-to-many relationship)

Includes:

- Primary and foreign key constraints
- `tables.active` flag for layout configuration
- Dynamic availability via time window overlap logic

---

### DAO Layer

- Pure JDBC (no ORM)
- Transaction-aware methods
- Shared `Connection` pattern for atomic operations
- Read projections via DTOs
- Overlap detection queries
- Capacity aggregation queries

---

### Service Layer (Business Logic)

- Transaction-safe reservation creation
- Default duration rule  
  If `endAt` is null → `startAt + 2 hours`
- Availability / overlap detection  
  `CANCELLED` and `NO_SHOW` do not block tables
- Capacity validation  
  `SUM(table.capacity) >= partySize`
- Reservation lifecycle:
  - `PENDING → CONFIRMED`
  - `PENDING/CONFIRMED → CANCELLED`
- Idempotent cancellation logic
- Manual validated table assignment
- Automatic greedy table assignment
- Active-only enforcement:
  - Inactive tables cannot be assigned
  - Inactive tables do not appear in availability results

---

### CLI Layer

Refactored into dedicated package:

- `MenuApp`
- `MenuPrinter`
- `Input`
- `Actions`

Features:

- Menu-driven workflow
- Early validation (fail-fast)
- Re-prompting on invalid input
- Transaction-safe operations exposed to CLI
- Admin configuration (activate/deactivate tables)

---

### Documentation

JavaDoc is fully supported.

Generate:

```
./gradlew javadoc
```

Then open:

```
build/docs/javadoc/index.html
```

PlantUML diagrams stored in:

```
docs/diagrams
```

Including:

- Architecture diagram
- Domain model
- ERD
- Sequence diagrams

---

## Project Structure

```
uoc-reservations-jdbc/
│
├─ src/main/java/edu/uoc/
│  ├─ cli/        # CLI layer (presentation)
│  ├─ service/    # Business logic + transactions
│  ├─ dao/        # JDBC persistence
│  ├─ model/      # Domain entities
│  ├─ dto/        # Query projections
│  ├─ db/         # Database connection
│  └─ Main.java   # Application entry point
│
└─ docs/
   └─ diagrams/   # PlantUML documentation
```

---

## Requirements

- Java JDK 17+
- PostgreSQL
- Gradle Wrapper (included)

---

## Database Setup

1. Create database:

```
uoc_databases
```

2. Run schema script:

```
01_schema/create_tables.sql
```

Tables created:

- customers
- tables
- reservations
- reservation_tables

Notes:

- `tables.active` controls structural availability
- Reservation availability is computed dynamically

---

## Configuration (Environment Variables)

Required:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

Example:

```
DB_URL=jdbc:postgresql://localhost:5432/uoc_databases
DB_USER=postgres
DB_PASSWORD=secret
```

---

## Run

From IntelliJ:
- Run `Main`

From terminal:

```
./gradlew run
```

---

## Transaction Model

- Transactions are owned exclusively by the service layer
- DAOs never commit or rollback
- Multi-step operations are atomic
- Failures trigger rollback automatically

This mirrors production backend architecture.

---

## Learning Goals

This project intentionally avoids frameworks to focus on:

- Relational modeling
- JDBC fundamentals
- Transaction boundaries
- Business rule enforcement
- Clean layered architecture

It serves as:

- A structured academic deliverable
- A backend architectural reference
- A foundation for future REST or web-layer integration

---

## License

Educational / personal learning project.
