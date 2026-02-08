# Reservation System — Development Roadmap

This document describes the planned evolution of the **uoc-reservations-jdbc** project after the `backbone-v2` milestone.

The goal of this roadmap is to:
- guide development in a controlled, incremental way
- keep architectural decisions explicit
- align implementation with real-world reservation systems
- serve as a learning reference for backend design (UOC context)

This roadmap is **directional**, not contractual.  
Items may evolve as understanding deepens.

---

## Backbone status

The following capabilities are already implemented and frozen in `backbone-v2`:

- Relational schema (customers, tables, reservations, reservation_tables)
- DAO layer with transaction-aware methods
- Service layer with transactional behavior
- Reservation creation with:
    - default duration logic
    - overlap detection
    - business validation
- Clean separation of:
    - persistence (DAO)
    - behavior (Service)
    - presentation (CLI)
- Design documentation (architecture, domain model, sequence, ERD)

All further work happens on feature branches built on top of this backbone.

---

## Milestone 1 — Availability Engine (core logic)

### F1 — Check table availability for a time window
**Goal**  
Enable the system to determine whether a table (or set of tables) is available for a requested time window.

**Key rules**
- A table is unavailable if there exists an overlapping reservation
- Only reservations with status ≠ CANCELLED, NO_SHOW block availability
- If end_at is NULL, assume start_at + 2 hours

**Notes**
- Overlap logic must be reused (single source of truth)
- Implemented in the service layer

---

### F2 — List available tables for a time window
**Goal**  
Allow querying which tables are available at a given date/time.

**Acceptance ideas**
- Input: startAt (+ optional endAt)
- Output: list of tables that:
    - are active
    - have no overlapping reservations
- Read-only operation (no assignments)

---

## Milestone 2 — Reservation lifecycle

### F3 — Cancel a reservation
**Goal**  
Allow cancelling reservations without deleting historical data.

**Rules**
- Status changes to CANCELLED
- Cancelled reservations do not block tables
- Data remains for auditing/history

---

### F4 — Confirm a reservation
**Goal**  
Support reservation confirmation as a lifecycle step.

**Rules**
- Valid transition: PENDING → CONFIRMED
- Invalid transitions rejected (e.g. CANCELLED → CONFIRMED)
- CONFIRMED reservations block availability

---

## Milestone 3 — Capacity & assignment logic

### F5 — Validate capacity on table assignment
**Goal**  
Prevent unrealistic bookings.

**Rules**
- Sum(capacity of assigned tables) ≥ partySize
- Validation enforced in the service layer
- Failure triggers transaction rollback

---

### F6 — Auto-assign tables (optional)
**Goal**  
Let the system choose tables automatically.

**Ideas**
- Pick smallest suitable combination
- Only among active and available tables

**Note**
- Advanced feature
- Optional, not required for core learning goals

---

## Milestone 4 — CLI UX improvements

### F7 — Early validation in CLI flows
**Goal**  
Avoid asking for unnecessary inputs when a fatal rule already fails.

**Examples**
- Invalid customer ID → stop flow immediately
- Unknown table code → stop flow immediately

---

### F8 — Re-prompt invalid inputs
**Goal**  
Improve user experience without changing business logic.

**Ideas**
- Re-ask only the invalid field
- Do not restart the entire flow

---

## Milestone 5 — Admin & configuration

### F9 — Activate / deactivate tables
**Goal**  
Allow configuration of physical restaurant layout.

**Rules**
- Inactive tables:
    - cannot be assigned
    - do not appear in availability searches
- Existing reservations remain valid (history)

---

## Out of scope (for now)

The following are intentionally postponed:
- Opening hours
- Deposits
- Authentication / users
- REST API or web UI
- Notifications

---

## Recommended execution order

To maintain clarity and avoid over-engineering:

1. Availability engine (F1–F2)
2. Reservation lifecycle (F3–F4)
3. Capacity validation (F5)
4. CLI UX improvements (F7–F8)
5. Admin configuration (F9)
6. Optional enhancements (F6)

---

## Notes

This roadmap supports:
- incremental learning
- clean architecture
- realistic system evolution

It may be updated as new insights emerge.
