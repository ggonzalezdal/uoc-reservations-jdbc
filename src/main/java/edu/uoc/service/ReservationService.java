package edu.uoc.service;

import edu.uoc.dao.CustomerDao;
import edu.uoc.dao.ReservationDao;
import edu.uoc.dao.ReservationTableDao;
import edu.uoc.dao.TableDao;
import edu.uoc.db.Database;
import edu.uoc.model.Reservation;
import edu.uoc.model.Table;

import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Service layer responsible for reservation business operations.
 *
 * <p>This class enforces business rules and coordinates multiple DAOs.</p>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Transaction orchestration (atomic reservation creation + table assignment)</li>
 *   <li>Validation (customer existence, active tables, capacity, time-window availability)</li>
 *   <li>Reservation lifecycle transitions (cancel / confirm)</li>
 * </ul>
 *
 * <p>Transaction strategy:</p>
 * <ul>
 *   <li>Service methods open a connection via {@link Database#getConnection()}.</li>
 *   <li>When atomicity is required, auto-commit is disabled and commit/rollback is controlled explicitly.</li>
 *   <li>DAOs remain transaction-aware and never commit/rollback themselves.</li>
 * </ul>
 *
 * @since 1.0
 */
public class ReservationService {

    private final CustomerDao customerDao = new CustomerDao();
    private final TableDao tableDao = new TableDao();
    private final ReservationDao reservationDao = new ReservationDao();
    private final ReservationTableDao reservationTableDao = new ReservationTableDao();

    /**
     * Creates a reservation and assigns the selected tables in one transaction.
     *
     * <p>Business rules:</p>
     * <ul>
     *   <li>If {@code endAt} is null → defaults to {@code startAt + 2 hours}</li>
     *   <li>{@code partySize} must be &gt; 0</li>
     *   <li>At least one table must be selected</li>
     *   <li>If {@code status} is null/blank → defaults to {@code PENDING}</li>
     *   <li>Customer must exist</li>
     *   <li>All tables must exist and be active</li>
     *   <li>Total capacity must be &gt;= party size</li>
     *   <li>Selected tables must not overlap with active reservations in the time window</li>
     * </ul>
     *
     * @param customerId customer identifier
     * @param startAt    reservation start date-time (required)
     * @param endAt      reservation end date-time (nullable, defaults to +2h)
     * @param partySize  number of guests
     * @param status     reservation status (nullable/blank defaults to PENDING)
     * @param notes      optional notes (nullable)
     * @param tableIds   selected table IDs (required, non-empty)
     * @return generated reservation ID
     * @throws IllegalArgumentException if inputs are invalid (e.g., missing customer, inactive tables)
     * @throws IllegalStateException if rule validation fails (e.g., overlap or insufficient capacity)
     * @throws RuntimeException on unexpected database/system failures
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

        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("endAt must be after startAt");
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

                // 3) Capacity validation
                validateCapacity(conn, tableIds, partySize);

                // 4) Availability (overlap)
                if (reservationDao.anyOverlappingForTables(conn, tableIds, startAt, endAt)) {
                    throw new IllegalStateException("Selected tables are not available in this time window");
                }

                // 5) Insert reservation
                Reservation reservation = new Reservation(
                        customerId,
                        startAt,
                        endAt,
                        partySize,
                        status,
                        notes
                );

                long reservationId = reservationDao.insert(conn, reservation);

                // 6) Assign tables
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
     * <p>Rules:</p>
     * <ul>
     *   <li>If {@code endAt} is null → defaults to {@code startAt + 2 hours}</li>
     *   <li>Tables are unavailable if any overlapping active reservation exists</li>
     *   <li>{@code CANCELLED} and {@code NO_SHOW} do not block availability</li>
     * </ul>
     *
     * @param tableIds tables to check
     * @param startAt  start of requested window (required)
     * @param endAt    end of requested window (nullable)
     * @return true if no overlap exists, false otherwise
     * @throws IllegalArgumentException if {@code startAt} is null
     * @throws RuntimeException if a database error occurs
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

        } catch (Exception e) {
            throw new RuntimeException("Failed to check availability", e);
        }
    }

    /**
     * Lists all active tables that are available for the given time window.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>If {@code endAt} is null → defaults to {@code startAt + 2 hours}</li>
     *   <li>Tables are unavailable if they have any overlapping active reservation</li>
     *   <li>{@code CANCELLED} and {@code NO_SHOW} do not block availability</li>
     * </ul>
     *
     * @param startAt start of requested window (required)
     * @param endAt   end of requested window (nullable)
     * @return list of available active tables
     * @throws IllegalArgumentException if {@code startAt} is null
     * @throws RuntimeException if a database error occurs
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

        } catch (Exception e) {
            throw new RuntimeException("Failed to list available tables", e);
        }
    }

    /**
     * Cancels a reservation without deleting historical data.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>Status becomes {@code CANCELLED}</li>
     *   <li>{@code cancelled_at} is set only on the first cancellation</li>
     *   <li>{@code cancellation_reason} is stored if provided (not overwritten if already set)</li>
     * </ul>
     *
     * @param reservationId reservation ID
     * @param reason        optional cancellation reason (nullable/blank allowed)
     * @return true if cancellation happened now, false if it was already cancelled
     * @throws IllegalArgumentException if reservation does not exist or ID is invalid
     * @throws IllegalStateException if cancellation fails unexpectedly
     * @throws RuntimeException if a database error occurs
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
                    conn.commit();
                    return false;
                }

                int updated = reservationDao.cancelById(conn, reservationId, reason);

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
     * <p>Rules:</p>
     * <ul>
     *   <li>Valid transition: {@code PENDING -> CONFIRMED}</li>
     *   <li>If already CONFIRMED: idempotent (returns false)</li>
     *   <li>Other statuses are rejected (e.g., CANCELLED, NO_SHOW)</li>
     * </ul>
     *
     * @param reservationId reservation ID
     * @return true if confirmed now, false if it was already confirmed
     * @throws IllegalArgumentException if reservation does not exist or ID is invalid
     * @throws IllegalStateException if status transition is invalid
     * @throws RuntimeException if a database error occurs
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
                    return false;
                }

                if (!"PENDING".equalsIgnoreCase(status)) {
                    throw new IllegalStateException("Invalid transition: " + status + " -> CONFIRMED");
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

    /**
     * Validated manual assignment of tables to an existing reservation (CLI option 8).
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>Reservation must exist</li>
     *   <li>Reservation status must not be CANCELLED or NO_SHOW</li>
     *   <li>All tables must exist and be active</li>
     *   <li>Total capacity must be &gt;= reservation party size</li>
     * </ul>
     *
     * <p>Transaction-safe: failure does not modify existing assignments.</p>
     *
     * @param reservationId reservation ID
     * @param tableIds      selected table IDs
     * @throws IllegalArgumentException if IDs are invalid / not found
     * @throws IllegalStateException if status/capacity rules are violated
     * @throws RuntimeException if a database error occurs
     */
    public void assignTablesToReservationValidated(long reservationId, List<Long> tableIds) {
        if (reservationId <= 0) {
            throw new IllegalArgumentException("reservationId must be > 0");
        }
        if (tableIds == null || tableIds.isEmpty()) {
            throw new IllegalArgumentException("At least one table must be selected");
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            try {
                var infoOpt = reservationDao.findCapacityInfoById(conn, reservationId);
                if (infoOpt.isEmpty()) {
                    throw new IllegalArgumentException("Reservation not found: " + reservationId);
                }

                var info = infoOpt.get();
                String status = info.status();

                if ("CANCELLED".equalsIgnoreCase(status) || "NO_SHOW".equalsIgnoreCase(status)) {
                    throw new IllegalStateException("Cannot assign tables to reservation with status: " + status);
                }

                if (!tableDao.allActiveExistByIds(conn, tableIds)) {
                    throw new IllegalArgumentException("Invalid or inactive table(s): " + tableIds);
                }

                validateCapacity(conn, tableIds, info.partySize());

                reservationTableDao.replaceAssignments(conn, reservationId, tableIds);

                conn.commit();

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to assign tables (validated)", e);
        }
    }

    /**
     * Creates a reservation and auto-assigns available tables using a greedy strategy.
     *
     * <p>Algorithm:</p>
     * <ol>
     *   <li>List available active tables for the given time window</li>
     *   <li>Sort tables deterministically (smallest capacity first, then numeric code, then lexical code)</li>
     *   <li>Greedily add tables until capacity covers {@code partySize}</li>
     *   <li>Delegate to {@link #createReservationWithTables(long, OffsetDateTime, OffsetDateTime, int, String, String, List)}</li>
     * </ol>
     *
     * <p>This is a simple baseline strategy, not an optimal knapsack solver.</p>
     *
     * @param customerId customer identifier
     * @param startAt    reservation start date-time (required)
     * @param endAt      reservation end date-time (nullable)
     * @param partySize  number of guests
     * @param status     reservation status (nullable/blank defaults to PENDING in underlying method)
     * @param notes      optional notes
     * @return generated reservation ID
     */
    public long createReservationAutoAssignTables(
            long customerId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            int partySize,
            String status,
            String notes
    ) {
        Objects.requireNonNull(startAt, "startAt cannot be null");

        if (endAt == null) {
            endAt = startAt.plusHours(2);
        }

        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }

        if (partySize <= 0) {
            throw new IllegalArgumentException("partySize must be > 0");
        }

        List<Table> available = listAvailableTables(startAt, endAt);

        List<Table> sorted = available.stream()
                .sorted(
                        Comparator.comparingInt(Table::getCapacity)
                                .thenComparingInt(t -> extractTableNumber(t.getCode()))
                                .thenComparing(Table::getCode, String.CASE_INSENSITIVE_ORDER)
                )
                .toList();

        List<Long> chosenIds = new ArrayList<>();
        int total = 0;

        for (Table t : sorted) {
            chosenIds.add(t.getId());
            total += t.getCapacity();
            if (total >= partySize) break;
        }

        if (total < partySize) {
            throw new IllegalStateException(
                    "No suitable combination of available tables for partySize=" + partySize
            );
        }

        return createReservationWithTables(
                customerId,
                startAt,
                endAt,
                partySize,
                status,
                notes,
                chosenIds
        );
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Validates that the total capacity of the selected active tables is sufficient.
     *
     * @param conn      existing database connection
     * @param tableIds  selected table IDs
     * @param partySize required party size
     * @throws IllegalStateException if capacity is insufficient
     */
    private void validateCapacity(Connection conn, List<Long> tableIds, int partySize) {
        int totalCapacity = tableDao.sumCapacityByIds(conn, tableIds);

        if (totalCapacity < partySize) {
            throw new IllegalStateException(
                    "Insufficient capacity: partySize=" + partySize + ", totalCapacity=" + totalCapacity
            );
        }
    }

    /**
     * Extracts the numeric part of a table code (e.g., "T12" -> 12).
     *
     * <p>If parsing fails, returns {@link Integer#MAX_VALUE} to ensure a stable sort order.</p>
     *
     * @param code table code
     * @return parsed number or {@code Integer.MAX_VALUE} on failure
     */
    private static int extractTableNumber(String code) {
        if (code == null) return Integer.MAX_VALUE;

        String digits = code.replaceAll("\\D+", "");
        if (digits.isEmpty()) return Integer.MAX_VALUE;

        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Returns all tables in the restaurant (active and inactive).
     * Thin pass-through to DAO. Useful for admin screens and diagnostics.
     *
     * @return list of all tables ordered by table code
     */
//    public List<Table> getAllTables() {
//        return tableDao.findAll();
//    }

}
