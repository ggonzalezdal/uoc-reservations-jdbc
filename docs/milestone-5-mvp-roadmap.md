# Milestone 5 – Web UI MVP Roadmap
uoc-reservations-jdbc

This document defines the functional and technical roadmap for Milestone 5:  
a minimal but realistic Web UI consuming the existing HTTP API.

The objective is to transform the current backend into a usable operational tool while preserving clean architecture and avoiding scope creep.

---

# 1. Goal of Milestone 5

Deliver a browser-based MVP that allows a restaurant to:

- View reservations for a selected day
- Filter reservations by status
- Create a reservation
- Confirm or cancel a reservation
- Check table availability for a time window

The frontend must consume the existing REST API without embedding business logic in the UI.

---

# 2. Guiding Principles

1. Backend remains authoritative for business rules
2. UI remains thin and API-driven
3. No duplication of validation logic in frontend
4. Add backend endpoints only when strictly required
5. Keep scope limited to operational MVP

---

# 3. MVP Functional Scope

## 3.1 Reservations Board (Primary Screen)

Default landing view.

Features:

- Date selector (default: today)
- Status filter:
    - All
    - PENDING
    - CONFIRMED
    - CANCELLED
- Reservation table with:
    - Time
    - Customer name
    - Party size
    - Status
    - Notes
- Inline actions:
    - Confirm (if PENDING)
    - Cancel (if not CANCELLED)
- Automatic refresh after actions

API used:

- GET /reservations?from=...&to=...
- GET /reservations?from=...&to=...&status=...
- POST /reservations/{id}/confirm
- POST /reservations/{id}/cancel

---

## 3.2 Create Reservation Screen

Accessible from sidebar or dashboard.

Fields:

- Customer selection (dropdown or simple input for MVP)
- Start datetime
- End datetime (optional)
- Party size
- Notes
- Table selection (optional for MVP; may rely on auto-assignment)

On submit:

- POST /reservations
- Display success message
- Redirect to Reservations Board
- Refresh board

Error handling:

- Display JSON error.message from API

---

## 3.3 Table Availability Helper

Used before creating reservation.

Inputs:

- Start datetime
- End datetime

Displays:

- List of available tables

API used:

- GET /tables/available?start=...&end=...

Future optional improvement:

- Add partySize query param (only if needed)

---

# 4. Backend Readiness Check

Already implemented:

- Transaction-safe reservation creation
- Overlap-based availability engine
- Capacity validation
- Lifecycle transitions
- Consistent JSON error model
- ISO-8601 OffsetDateTime
- Filtering by window and status

Possible future additions (only if required):

- GET /reservations/{id} (include assigned tables)
- POST /customers
- GET /tables/available with partySize parameter

These are NOT part of MVP unless UI requires them.

---

# 5. UI Technical Structure

Location:

src/main/resources/public/

Structure:

- index.html
- css/styles.css
- js/app.js
- js/state.js
- js/render.js
- js/actions.js
- images/

Frontend responsibilities:

- Fetch data from API
- Render reservations dynamically
- Handle button actions
- Display errors
- Format ISO dates for display

No business logic in frontend.

---

# 6. User Flows (MVP)

## Flow 1 – View Today’s Reservations

1. Load page
2. UI calculates today’s from/to window
3. Fetch reservations
4. Render table

---

## Flow 2 – Confirm Reservation

1. Click confirm
2. POST /reservations/{id}/confirm
3. Refresh board

---

## Flow 3 – Cancel Reservation

1. Click cancel
2. POST /reservations/{id}/cancel
3. Refresh board

---

## Flow 4 – Create Reservation

1. Open form
2. Fill fields
3. Submit
4. Handle 201 or error
5. Redirect and refresh board

---

# 7. MVP Acceptance Criteria

Milestone 5 is complete when:

- Reservations board loads correctly
- Status filter works
- Confirm action updates UI and backend
- Cancel action updates UI and backend
- Reservation creation works end-to-end
- API errors display correctly in UI
- No business logic is duplicated in frontend
- Backend architecture remains unchanged

---

# 8. Explicitly Out of Scope

- Editing reservations
- Drag-and-drop calendar
- Multi-day calendar view
- Authentication
- Role management
- Customer management UI
- Reporting / analytics
- Real-time WebSockets

These belong to later milestones.

---

# 9. Expected Result

By the end of Milestone 5, the system should feel like:

A small, real operational reservation tool with:

- Clean backend architecture
- Transaction safety
- REST API
- Functional web interface
- Clear separation of concerns

This milestone transforms the project from academic backend to usable product foundation.

---

End of Milestone 5 Roadmap
