# uoc-reservations-jdbc — Project Roadmap

This roadmap describes the planned evolution of the **uoc-reservations-jdbc** project from a JDBC backbone into a usable reservation application with a simple web UI.

It is a **directional plan** (not a contract). Scope and priorities may evolve as learning progresses.

---

## Current status (Backbone)

Stable milestones:
- `backbone-v1` — initial JDBC + schema foundation
- `backbone-v2` — transaction-aware DAOs + service layer + diagrams

Implemented capabilities:
- PostgreSQL schema:
    - customers
    - tables (physical setup; includes active flag)
    - reservations
    - reservation_tables (many-to-many assignments)
- DAO layer:
    - mapping + SQL access
    - transaction-aware methods (shared Connection pattern)
- Service layer:
    - transaction-safe reservation creation with table assignments
    - default duration rule (if endAt is null → startAt + 2 hours)
    - overlap detection (availability conflict)
    - clear business error propagation
- CLI:
    - demo workflows and basic operations
- Documentation:
    - Architecture, Domain Model, Sequence, ERD diagrams (PlantUML)

---

## Roadmap principles

- Keep the core truth in the database + service layer (business rules).
- UI/HTTP layers must remain thin.
- Prefer incremental milestones, each delivering real functionality.
- Avoid over-polish before behavior is correct.

---

## Milestone 1 — Availability engine (backend)

Goals:
- query availability reliably
- reuse overlap logic as the single source of truth

Planned features:
- Check availability for a time window (single table or set of tables)
- List available tables for a time window
- Capacity validation for assigned tables (sum capacities ≥ party size)

Deliverable:
- service methods + DAO queries that support real availability reasoning

---

## Milestone 2 — Reservation lifecycle (backend)

Goals:
- make reservations behave like real operational entities

Planned features:
- confirm reservation (PENDING → CONFIRMED)
- cancel reservation (→ CANCELLED)
- optional: mark NO_SHOW

Deliverable:
- service methods enforcing valid transitions
- availability queries respect status rules

---

## Milestone 3 — CLI usability improvements (optional but useful)

Goals:
- reduce friction while testing and learning

Planned features:
- early validation (stop flow early on fatal errors)
- re-prompt invalid fields rather than restarting full flows
- consistent error formatting (portable console output)

Deliverable:
- improved CLI flows without changing service logic

---

## Milestone 4 — Thin HTTP API (bridge to UI)

Goals:
- expose the backend behavior via a minimal web API
- keep frameworks minimal and educational

Planned approach:
- lightweight HTTP layer (e.g., Javalin or SparkJava; alternative: Servlets)
- endpoints for:
    - customers
    - availability search
    - create reservation with tables
    - cancel/confirm reservation
    - list reservations

Deliverable:
- a runnable backend server that a frontend can call via fetch()

---

## Milestone 5 — WAR-style web UI (frontend)

Goals:
- a usable “real app” interface inspired by WAR Assistant aesthetics
- minimal JS wiring, consistent layout, no heavy frontend framework required

Minimum lovable UI:
- reservations list dashboard
- create reservation flow:
    - choose date/time (+ optional end)
    - party size
    - show available tables
    - select tables / create reservation
- confirm / cancel actions

Deliverable:
- static HTML/CSS pages + vanilla JS fetch() to the API
- consistent styling aligned with WAR Assistant look & feel

---

## Milestone 6 — Admin / configuration (later)

Goals:
- manage physical table setup and restaurant configuration

Planned features:
- activate/deactivate tables
- manage table capacity and codes
- optional: zones (inside/terrace), combinability rules

Deliverable:
- admin endpoints + admin UI (optional)

---

## Out of scope (for now)

- authentication and roles
- deposits / payments
- notifications
- opening-hours engine
- production deployment

---

## Tracking work

- High-level plan: this file (`docs/ROADMAP.md`)
- Implementation work: feature branches + Git tags
- Optional: GitHub Issues (one issue per feature / milestone)
