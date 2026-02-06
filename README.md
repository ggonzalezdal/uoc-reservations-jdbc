# uoc-reservations-jdbc

Educational Java backend project built for the **UOC – Introduction to Databases** course.

This repository implements a **clean JDBC-based backend** for a restaurant reservations domain, designed to understand how Java applications interact with a relational database **without frameworks** (no Spring, no JPA).

It is intentionally structured as a **backbone project**: correct, readable, and extensible, but not yet feature-complete.

---

## Purpose of this project

The main goals of this project are to:

- Understand JDBC at a low level (connections, prepared statements, result sets)
- Practice clean separation of concerns:
  - domain models
  - data access (DAO)
  - DTOs for read models
  - application entry point (CLI)
- Learn how to:
  - read data from a PostgreSQL database
  - insert data safely
  - map relational rows to Java objects
  - prepare for transaction-safe service layers

This project is designed as a **foundation** for later extensions, not as a final restaurant system.

---

## Project structure

- `src/main/java/edu/uoc`
  - `Main.java` — CLI entry point
  - `db`
    - `Database.java` — JDBC connection helper
  - `model` — Domain models (1:1 with DB tables)
    - `Customer.java`
    - `Reservation.java`
    - `Table.java`
  - `dto` — Read-only DTOs for listing views
    - `ReservationListItem.java`
  - `dao` — Data Access Objects (JDBC)
    - `CustomerDao.java`
    - `ReservationDao.java`
    - `TableDao.java`
    - `ReservationTableDao.java`

---

## Domain model overview

The Java model mirrors the database schema:

- **Customer**
  - id, fullName, phone, email, createdAt

- **Reservation**
  - reservationId, customerId
  - startAt, endAt
  - partySize, status, notes
  - createdAt

- **Table**
  - id, code, capacity, active

Relationships are handled explicitly through DAOs and IDs, not via ORM magic.

---

## DAO layer responsibilities

Each DAO is responsible for **one table only**:

- **CustomerDao**
  - insert customers
  - find all / find by id

- **ReservationDao**
  - insert reservations
  - list reservations joined with customers
  - map timestamps to `OffsetDateTime`

- **TableDao**
  - read physical restaurant tables
  - lookup by id or table code

- **ReservationTableDao**
  - manage the many-to-many relationship
  - assign tables to reservations
  - designed to support transaction reuse

DAOs do **not** contain business rules. They only persist and retrieve data.

---

## CLI (Main.java)

The CLI is intentionally simple and synchronous.  
It exists to **exercise the DAOs**, not to be a real user interface.

Current capabilities:

- list customers
- add customers
- list reservations
- add reservations (with optional end time and notes)
- list physical tables
- assign tables to an existing reservation
- inspect table assignments for a reservation

This makes it easy to validate each layer independently.

---

## Transaction awareness (important)

The project is **prepared** for transaction-safe operations:

- DAOs expose methods that accept an existing `Connection`
- This allows future service methods to:
  - disable auto-commit
  - coordinate multiple DAOs
  - commit or rollback atomically

At this stage, there is **no service layer yet**.  
That is intentional and comes next.

---

## What is intentionally NOT implemented yet

This is a backbone project. The following are deliberately deferred:

- transaction-safe “create reservation + assign tables” service
- table availability checks
- prevention of overlapping reservations
- capacity optimization rules
- automatic table assignment
- REST API or UI

These will be layered **on top** of this backbone.

---

## Database dependency

This project expects the companion PostgreSQL database project **uoc-reservations-db** to be:

- created
- seeded
- running

Database access is configured via environment variables (see `Database.java`).

---

## Why no frameworks?

This is a learning-oriented project.

Using raw JDBC here helps to:

- understand what frameworks abstract away
- reason about SQL execution
- debug database interactions confidently

Frameworks can be added later **after** the fundamentals are solid.

---

## Status

This repository represents a **stable JDBC backbone**.

It is suitable as:

- a reference implementation
- a base for further coursework
- a starting point for more advanced architectures

---

### Next step

After committing this README, the project will be **frozen as a backbone version** (tag + branch) before introducing the service layer.

