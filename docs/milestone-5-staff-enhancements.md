# Milestone 5 – Staff Realism Enhancements Roadmap
uoc-reservations-jdbc

This document defines the next incremental improvements to the Milestone 5 Web UI.

The objective is to evolve the current operational MVP into a more realistic restaurant front-desk tool without introducing scope creep or architectural changes.

All enhancements must:
- Preserve thin UI principles
- Keep business logic in backend
- Avoid unnecessary endpoints
- Remain incremental and reversible

---

# 1. Current State (Baseline)

The system already supports:

- List reservations by date window
- Filter by status (PENDING, CONFIRMED, CANCELLED, NO_SHOW)
- Create reservations (Auto / Manual table assignment)
- Confirm reservation
- Cancel reservation
- Availability preview
- Customer listing for dropdown selection
- Clean REST API + transactional service layer

This is a functional operational MVP.

---

# 2. Goal of This Enhancement Phase

Make the UI behave like a real restaurant front-desk tool by adding small, high-impact features that improve operational realism.

No authentication, reporting, analytics, or editing flows will be added in this phase.

---

# 3. Enhancements Overview

## 3.1 Add NO_SHOW Action

### Objective
Allow staff to mark a reservation as NO_SHOW.

### Why It Matters
In real operations:
- Guests may not arrive.
- Staff must record this.
- It affects metrics and availability history.

### Backend Changes
Add endpoint:

POST /reservations/{id}/no-show

Rules:
- Allowed from: PENDING, CONFIRMED
- Not allowed from: CANCELLED, NO_SHOW

Service layer remains authoritative for status transitions.

### Frontend Changes
- Add "No-show" button in reservation row.
- Enable only for PENDING or CONFIRMED.
- Refresh table after action.

---

## 3.2 "Today" Quick Action + Auto-Load

### Objective
Improve daily workflow usability.

### Why It Matters
Staff primarily operate on "today".
Requiring manual date selection slows workflow.

### Changes (Frontend Only)

- Add "Today" button near date selector.
- When Reservations view opens:
    - Auto-load today's reservations.
- Persist last used filters (date, status, sort) in localStorage.

No backend changes required.

---

## 3.3 Display Assigned Tables

### Objective
Show where guests are seated.

### Why It Matters
Front desk must know table allocation.

### Backend Changes
Add:

GET /reservations/{id}

Return:
- Reservation fields
- Assigned tables list (e.g., ["T1", "T3"])

This requires a JOIN with reservation_tables and tables.

### Frontend Options
Option A (MVP recommended):
- Add "Tables" column in reservation table.

Option B:
- Add small details modal per reservation.

MVP implementation will use Option A.

---

## 3.4 Customer Lookup Usability

### Objective
Improve reservation creation workflow.

### Why It Matters
Selecting customers by ID is unrealistic.
Front desk works by name.

### Changes (Frontend Only)

- Add search input above customer select.
- Filter loaded customers client-side.
- Keep dropdown behavior unchanged.

No backend changes required.

---

# 4. Implementation Order

To minimize risk and preserve stability:

1. UI-only improvements
    - Today button
    - Auto-load on view open
    - localStorage persistence

2. Add NO_SHOW endpoint + button

3. Add assigned tables endpoint + UI column

4. Add customer search filter

Each step must be committed independently.

---

# 5. Acceptance Criteria

Enhancement phase is complete when:

- Reservations auto-load on opening view
- Today button resets to current date
- Filters persist across reload
- NO_SHOW action updates status correctly
- Assigned tables are visible in UI
- Customer dropdown supports name search
- No business logic duplicated in frontend
- Backend architecture remains unchanged

---

# 6. Explicitly Out of Scope

The following remain outside this phase:

- Editing reservations
- Authentication / roles
- Walk-in seating workflow
- Multi-day calendar UI
- Analytics dashboard
- Reporting metrics
- Guest-facing public booking form

These belong to future milestones.

---

# 7. Expected Result

After this phase, the system should feel like:

A small but realistic restaurant reservation tool with:

- Operational daily workflow
- Status lifecycle completeness
- Table visibility
- Efficient front-desk usability
- Clean layered architecture
- REST-driven thin UI

This marks the transition from academic MVP to practical operational prototype.
