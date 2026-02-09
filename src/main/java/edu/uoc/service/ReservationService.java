package edu.uoc.service;

import edu.uoc.dao.CustomerDao;
import edu.uoc.dao.ReservationDao;
import edu.uoc.dao.ReservationTableDao;
import edu.uoc.dao.TableDao;
import edu.uoc.db.Database;
import edu.uoc.model.Reservation;
import edu.uoc.model.Table;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ReservationService {

    private final CustomerDao customerDao = new CustomerDao();
    private final TableDao tableDao = new TableDao();
    private final ReservationDao reservationDao = new ReservationDao();
    private final ReservationTableDao reservationTableDao = new ReservationTableDao();

    /**
     * Creates a reservation AND assigns the selected tables in ONE transaction.
     * Business rule: if endAt is null -> startAt + 2 hours.
     *
     * @return generated reservationId
     */
    public long createReservationWithTables(
            long customerId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            int partySize,
            String status,
            String notes,
            List<Long> tableIds
    ) {

        Objects.requireNonNull(startAt, "startAt cannot be null");

        if (endAt == null) {
            endAt = startAt.plusHours(2);
        }

        if (partySize <= 0) {
            throw new IllegalArgumentException("partySize must be > 0");
        }

        if (tableIds == null || tableIds.isEmpty()) {
            throw new IllegalArgumentException("At least one table must be selected");
        }

        if (status == null || status.isBlank()) {
            status = "PENDING";
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1) Customer exists
                if (!customerDao.existsById(conn, customerId)) {
                    throw new IllegalArgumentException("Customer not found: " + customerId);
                }

                // 2) Tables exist and are active
                if (!tableDao.allActiveExistByIds(conn, tableIds)) {
                    throw new IllegalArgumentException("Invalid or inactive table(s): " + tableIds);
                }

                // 3) Availability (overlap)
                if (reservationDao.anyOverlappingForTables(conn, tableIds, startAt, endAt)) {
                    throw new IllegalStateException("Selected tables are not available in this time window");
                }

                // 4) Insert reservation
                Reservation reservation = new Reservation(
                        customerId,
                        startAt,
                        endAt,
                        partySize,
                        status,
                        notes
                );

                long reservationId = reservationDao.insert(conn, reservation);

                // 5) Assign tables
                reservationTableDao.addAssignments(conn, reservationId, tableIds);

                conn.commit();
                return reservationId;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e; // propagate business errors unchanged
        } catch (Exception e) {
            throw new RuntimeException("Failed to create reservation with tables", e);
        }
    }

    /**
     * Checks whether the given tables are available for a time window.
     *
     * Business rules:
     * - If endAt is null, defaults to startAt + 2 hours
     * - Tables are unavailable if any overlapping reservation exists
     * - CANCELLED and NO_SHOW reservations do not block availability
     *
     * @return true if tables are available, false otherwise
     */
    public boolean isAvailableForTables(
            List<Long> tableIds,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        if (startAt == null) {
            throw new IllegalArgumentException("startAt cannot be null");
        }

        OffsetDateTime effectiveEndAt =
                (endAt != null) ? endAt : startAt.plusHours(2);

        try (Connection conn = Database.getConnection()) {

            boolean overlapExists =
                    reservationDao.anyOverlappingForTables(
                            conn,
                            tableIds,
                            startAt,
                            effectiveEndAt
                    );

            return !overlapExists;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check availability", e);
        }
    }

    /**
     * Lists all active tables that are available for the given time window.
     *
     * Business rules:
     * - If endAt is null, defaults to startAt + 2 hours
     * - Tables are unavailable if they have any overlapping reservation
     * - CANCELLED and NO_SHOW do not block availability
     */
    public List<Table> listAvailableTables(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt == null) {
            throw new IllegalArgumentException("startAt cannot be null");
        }

        OffsetDateTime effectiveEndAt = (endAt != null) ? endAt : startAt.plusHours(2);

        try (Connection conn = Database.getConnection()) {

            // 1) Candidates = active tables
            List<Table> activeTables = tableDao.findAllActive(conn);

            List<Long> ids = activeTables.stream()
                    .map(Table::getId)
                    .toList();

            // 2) Blocked ids = any table with an overlapping reservation
            Set<Long> blockedIds = reservationDao.findOverlappingTableIds(
                    conn, ids, startAt, effectiveEndAt
            );

            // 3) Available = active - blocked
            return activeTables.stream()
                    .filter(t -> !blockedIds.contains(t.getId()))
                    .toList();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to list available tables", e);
        }
    }

    /**
     * Cancels a reservation without deleting historical data.
     *
     * Rules:
     * - status -> CANCELLED
     * - cancelled_at -> set on first cancellation only
     * - cancellation_reason -> stored if provided (not overwritten if already set)
     *
     * @return true if cancellation happened now, false if it was already cancelled
     */
    public boolean cancelReservation(long reservationId, String reason) {

        if (reservationId <= 0) {
            throw new IllegalArgumentException("reservationId must be > 0");
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            try {
                var statusOpt = reservationDao.findStatusById(conn, reservationId);

                if (statusOpt.isEmpty()) {
                    throw new IllegalArgumentException("Reservation not found: " + reservationId);
                }

                if ("CANCELLED".equalsIgnoreCase(statusOpt.get())) {
                    conn.commit(); // nothing to do, but keep transaction style consistent
                    return false;
                }

                int updated = reservationDao.cancelById(conn, reservationId, reason);

                // Should be 1 if it existed and wasn't cancelled
                if (updated != 1) {
                    throw new IllegalStateException("Cancel failed unexpectedly for reservation: " + reservationId);
                }

                conn.commit();
                return true;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to cancel reservation", e);
        }
    }

    /**
     * Confirms a reservation as a lifecycle transition.
     *
     * Rules:
     * - Valid transition: PENDING -> CONFIRMED
     * - Other transitions rejected (CANCELLED/NO_SHOW/CONFIRMED)
     *
     * @return true if confirmed now, false if it was already CONFIRMED
     */
    public boolean confirmReservation(long reservationId) {

        if (reservationId <= 0) {
            throw new IllegalArgumentException("reservationId must be > 0");
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            try {
                var statusOpt = reservationDao.findStatusById(conn, reservationId);

                if (statusOpt.isEmpty()) {
                    throw new IllegalArgumentException("Reservation not found: " + reservationId);
                }

                String status = statusOpt.get();

                if ("CONFIRMED".equalsIgnoreCase(status)) {
                    conn.commit();
                    return false; // idempotent: already confirmed
                }

                if (!"PENDING".equalsIgnoreCase(status)) {
                    throw new IllegalStateException(
                            "Invalid transition: " + status + " -> CONFIRMED"
                    );
                }

                int updated = reservationDao.confirmById(conn, reservationId);

                if (updated != 1) {
                    throw new IllegalStateException("Confirm failed unexpectedly for reservation: " + reservationId);
                }

                conn.commit();
                return true;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to confirm reservation", e);
        }
    }

}
