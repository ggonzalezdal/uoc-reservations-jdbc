# uoc-reservations-jdbc

Educational Java backend project built for **UOC – Introduction to Databases**.

This repository implements a **clean JDBC-based backend** for a restaurant reservations domain, designed to understand how Java applications interact with a relational database **without frameworks** (no Spring, no JPA).

It is intentionally structured as a **backbone project**: correct, readable, and extensible — with clear separation between **DAO**, **Service Layer (behavior)**, and **CLI (presentation)**.

---

## What this project covers

### Implemented
- PostgreSQL schema with:
  - customers
  - tables (physical restaurant tables configuration)
  - reservations
  - reservation_tables (many-to-many assignment)
- DAO layer (SQL + mapping):
  - CRUD-style reads/inserts
  - transaction-aware DAO methods (shared Connection pattern)
- Service layer (business behavior):
  - ReservationService.createReservationWithTables(...)
  - transaction-safe create reservation + assign tables
  - default duration rule: if endAt is null → startAt + 2 hours
  - overlap / availability check for selected tables
  - clean propagation of business errors (IllegalArgumentException / IllegalStateException)
- CLI menu:
  - demo-friendly options for customers, reservations, tables
  - transaction-safe workflow via option 9
- Design documentation:
  - PlantUML diagrams in docs/diagrams

### Planned / next iterations
- Smarter CLI UX (re-prompt only invalid fields)
- Availability search (list available tables by time window / party size)
- Capacity-aware automatic table selection
- Update table configuration (tables.active) from CLI/admin flows
- Reservation updates (cancel / reschedule)

---

## Project structure

src/main/java/edu/uoc
- db        Database connection (env-based)
- model     Domain entities (Customer, Reservation, Table)
- dto       Read projections (ReservationListItem)
- dao       DAOs (SQL + mapping, transaction-aware)
- service   Service layer (business behavior + transactions)
- Main.java CLI entry point

docs
- diagrams  PlantUML diagrams (architecture, domain, sequence, ERD)

---

## Requirements
- Java JDK 17 or higher
- PostgreSQL
- Gradle wrapper (included)

---

## Database setup

1. Create database  
   Example database name:
- uoc_databases

2. Create tables  
   Run the DDL script:
- 01_schema/create_tables.sql

This creates:
- customers
- tables
- reservations
- reservation_tables

Note:
- tables.active models whether a physical table can be used at all (salon configuration)
- availability at a given time is computed dynamically via reservation overlap logic

---

## Configuration (Environment Variables)

The application reads database configuration from environment variables.

Required variables:
- DB_URL
- DB_USER
- DB_PASSWORD

Example DB_URL:
- jdbc:postgresql://localhost:5432/uoc_databases

---

## Run

From IntelliJ:
- Run Main

From terminal:
- ./gradlew run

---

## CLI overview

Customers:
- list customers
- add customer
- find customer by id

Reservations:
- list reservations
- add reservation (basic insert)

Tables:
- list tables

Assignments:
- show table assignments for a reservation
- assign tables to an existing reservation

Option 9 (recommended):
- create reservation + assign tables in one transaction
- validates:
  - customer exists
  - tables exist and are active
  - tables are available (no overlap)
- commits only if everything succeeds, otherwise rollback

---

## Transaction safety

The service layer owns transaction boundaries:
- opens one Connection
- disables auto-commit
- performs validations and inserts using the same connection
- commits on success
- rollbacks on failure

DAOs support shared-connection usage so multi-step operations are atomic.

---

## Design diagrams

Stored in:
- docs/diagrams

Recommended set:
1. 01-architecture.puml
2. 02-domain-model.puml
3. 03-createReservationWithTables-sequence.puml
4. 04-erd.puml

---

## Learning goals

This project is framework-free by design to focus on:
- relational modeling (PK, FK, constraints)
- JDBC fundamentals
- transaction management
- clean separation of concerns

---

## License
Educational / personal learning project.
