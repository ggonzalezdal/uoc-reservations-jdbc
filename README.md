# uoc-reservations-jdbc

Educational Java backend project built for **UOC – Introduction to Databases**.

This repository implements a **clean, framework-free JDBC backend**
for a restaurant reservation system, designed to deeply understand how
Java applications interact with a relational database **without Spring or JPA**.

The project follows a layered architecture with strict separation between:

- **DAO** (persistence, SQL, mapping)
- **Service layer** (business rules + transactions)
- **CLI** (presentation / user interaction)

---

## Project status

✅ **Stable and usable**

The project has reached a first complete milestone covering:
- availability logic
- reservation lifecycle
- capacity validation
- admin configuration
- improved CLI UX

This state is frozen and published as a GitHub Release.

Further work will build incrementally on top of this foundation.

---

## What this project covers

### Implemented features

#### Data model
- PostgreSQL schema:
  - `customers`
  - `tables` (physical restaurant tables)
  - `reservations`
  - `reservation_tables` (many-to-many assignment)
- Referential integrity (PK/FK)
- `tables.active` for physical layout configuration

#### DAO layer
- SQL-based DAOs (no ORM)
- Transaction-aware DAO methods
- Shared `Connection` pattern for atomic operations
- Read projections via DTOs

#### Service layer (business logic)
- Transaction-safe reservation creation
- Default duration rule:
  - if `endAt` is null → `startAt + 2 hours`
- Availability / overlap detection
  - `CANCELLED` and `NO_SHOW` do not block tables
- Capacity validation:
  - `SUM(table.capacity) >= partySize`
- Reservation lifecycle:
  - confirm (`PENDING → CONFIRMED`)
  - cancel (history preserved)
- Manual table assignment with validation
- Automatic table assignment (greedy strategy)
- Active-only enforcement:
  - inactive tables cannot be assigned
  - inactive tables do not appear in availability searches

#### CLI (presentation)
- Menu-driven CLI
- Early validation (fail fast on invalid IDs)
- Re-prompting for invalid inputs
- Admin actions:
  - activate / deactivate tables
- Transaction-safe workflows exposed via CLI

#### Design documentation
- PlantUML diagrams stored in `docs/diagrams`
  - architecture
  - domain model
  - sequence diagrams
  - ERD

---

## Milestones & releases

- **backbone-v1** — initial JDBC skeleton
- **backbone-v2** — transaction-safe service backbone
- **milestone-availability-and-ux** — fully usable reservation system
  - availability engine
  - capacity validation
  - lifecycle management
  - CLI UX improvements
  - admin configuration

Each milestone is tagged and released on GitHub.

---

## Project structure

src/main/java/edu/uoc
- db        Database connection (env-based)
- model     Domain entities
- dto       Read-only projections
- dao       JDBC DAOs (SQL + mapping)
- service   Business logic + transactions
- Main.java CLI entry point

docs/
- diagrams  PlantUML diagrams

---

## Requirements

- Java JDK 17+
- PostgreSQL
- Gradle Wrapper (included)

---

## Database setup

1. Create database  
   Example:
   uoc_databases

2. Create tables  
   Run:
   01_schema/create_tables.sql

Tables created:
- `customers`
- `tables`
- `reservations`
- `reservation_tables`

Notes:
- `tables.active` controls whether a table can be used at all
- Availability is computed dynamically via overlap logic

---

## Configuration (Environment Variables)

Required:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

Example:
DB_URL=jdbc:postgresql://localhost:5432/uoc_databases

---

## Run

From IntelliJ:
- Run `Main`

From terminal:
./gradlew run

---

## Transaction model

- Transactions are owned by the **service layer**
- DAOs never commit or rollback
- Multi-step operations are fully atomic
- Failures trigger rollback automatically

This mirrors real-world backend design.

---

## Learning goals

This project intentionally avoids frameworks to focus on:

- relational modeling
- JDBC fundamentals
- transaction boundaries
- business rule enforcement
- clean architecture

It serves both as a **learning artifact** and a **reference backend**.

---

## License

Educational / personal learning project.
