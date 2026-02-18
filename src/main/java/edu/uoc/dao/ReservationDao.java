package edu.uoc.dao;

import edu.uoc.db.Database;
import edu.uoc.dto.ReservationListItem;
import edu.uoc.model.Reservation;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Data Access Object for {@link Reservation} and reservation-related queries.
 *
 * <p>This DAO provides:</p>
 * <ul>
 *   <li>Reservation listing with customer name (DTO view)</li>
 *   <li>Transaction-aware reservation insert</li>
 *   <li>Overlap checks for assigned tables</li>
 *   <li>Status transitions (cancel/confirm) with audit fields</li>
 * </ul>
 *
 * <p>Transaction-aware methods accept a {@link Connection} and do not manage
 * commit/rollback. Non-transactional convenience overloads open their own connection.</p>
 *
 * @since 1.0
 */
public class ReservationDao {

    // -------------------------------------------------------------------------
    // Queries / Reads
    // -------------------------------------------------------------------------

    /**
     * Retrieves all reservations joined with customer full name.
     *
     * <p>Returns a list of {@link ReservationListItem} DTOs ordered by start time.</p>
     *
     * @return list of reservation list items (empty if none exist)
     * @throws RuntimeException if a database error occurs
     */
    public List<ReservationListItem> findAll() {
        String sql = """
            SELECT
              r.reservation_id,
              r.customer_id,
              c.full_name,
              r.start_at,
              r.end_at,
              r.party_size,
              r.status,
              r.notes,
              r.created_at
            FROM reservations r
            JOIN customers c ON c.customer_id = r.customer_id
            ORDER BY r.start_at
            """;

        List<ReservationListItem> results = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                results.add(mapReservationListItem(rs));
            }

            return results;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations", e);
        }
    }

    /**
     * Checks whether a reservation exists by its identifier.
     *
     * <p>Non-transactional: opens and closes its own connection.</p>
     *
     * @param reservationId reservation ID
     * @return true if the reservation exists, false otherwise
     * @throws RuntimeException if a database error occurs
     */
    public boolean existsById(long reservationId) {
        String sql = "SELECT 1 FROM reservations WHERE reservation_id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, reservationId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check reservation existence: " + reservationId, e);
        }
    }

    /**
     * Returns the current status for a reservation, or empty if not found.
     *
     * <p>Transaction-aware: uses the provided connection.</p>
     *
     * @param conn          existing database connection
     * @param reservationId reservation ID
     * @return optional status string
     * @throws RuntimeException if a database error occurs
     */
    public Optional<String> findStatusById(Connection conn, long reservationId) {
        String sql = "SELECT status FROM reservations WHERE reservation_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reservationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.ofNullable(rs.getString("status"));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservation status for id=" + reservationId, e);
        }
    }

    // -------------------------------------------------------------------------
    // Inserts
    // -------------------------------------------------------------------------

    /**
     * Inserts a reservation using the provided connection (transaction-aware).
     *
     * <p>This method returns the generated {@code reservation_id} and also sets:</p>
     * <ul>
     *   <li>{@code reservationId} into the provided {@link Reservation} object</li>
     *   <li>{@code createdAt} into the provided {@link Reservation} object</li>
     * </ul>
     *
     * <p>This method does not commit/rollback. The caller controls the transaction.</p>
     *
     * @param conn existing database connection (transaction context)
     * @param r    reservation to insert
     * @return generated reservation ID
     * @throws RuntimeException if insertion fails
     */
    public long insert(Connection conn, Reservation r) {
        String sql = """
            INSERT INTO reservations (customer_id, start_at, end_at, party_size, status, notes)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING reservation_id, created_at
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, r.getCustomerId());
            ps.setObject(2, r.getStartAt());   // OffsetDateTime -> timestamptz
            ps.setObject(3, r.getEndAt());     // nullable OK
            ps.setInt(4, r.getPartySize());
            ps.setString(5, r.getStatus());
            ps.setString(6, r.getNotes());     // nullable OK

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Insert succeeded but no reservation_id returned.");
                }

                long id = rs.getLong("reservation_id");
                r.setReservationId(id);

                OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);
                r.setCreatedAt(createdAt);

                return id;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }
    }

    /**
     * Convenience overload for inserting a reservation without an explicit transaction.
     *
     * <p>Opens a connection internally and delegates to {@link #insert(Connection, Reservation)}.</p>
     *
     * @param r reservation to insert
     * @return generated reservation ID
     * @throws RuntimeException if insertion fails
     */
    public long insert(Reservation r) {
        try (Connection conn = Database.getConnection()) {
            return insert(conn, r);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }
    }

    // -------------------------------------------------------------------------
    // Overlap checks (table availability)
    // -------------------------------------------------------------------------

    /**
     * Checks if any active reservation overlaps the given time window
     * for any of the provided table IDs.
     *
     * <p>Overlap rule used:</p>
     * <ul>
     *   <li>ExistingStart &lt; NewEnd</li>
     *   <li>NewStart &lt; ExistingEnd</li>
     * </ul>
     *
     * <p>Reservations with status {@code CANCELLED} or {@code NO_SHOW} are ignored.</p>
     *
     * <p>If {@code endAt} is null in an existing reservation, this query treats it as
     * {@code start_at + 2 hours} using {@code COALESCE}.</p>
     *
     * <p>Transaction-aware: uses provided connection.</p>
     *
     * @param conn     existing database connection
     * @param tableIds table IDs to check (empty => no overlap)
     * @param startAt  new reservation start
     * @param endAt    new reservation end
     * @return true if any overlap exists, false otherwise
     * @throws RuntimeException if a database error occurs
     */
    public boolean anyOverlappingForTables(
            Connection conn,
            List<Long> tableIds,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        if (tableIds == null || tableIds.isEmpty()) {
            return false; // no tables => nothing can overlap
        }

        String sql = """
            SELECT 1
            FROM reservation_tables rt
            JOIN reservations r ON r.reservation_id = rt.reservation_id
            WHERE rt.table_id = ANY (?::bigint[])
              AND r.status NOT IN ('CANCELLED', 'NO_SHOW')
              AND r.start_at < ?
              AND ? < COALESCE(r.end_at, r.start_at + interval '2 hours')
            LIMIT 1
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setArray(1, conn.createArrayOf("bigint", tableIds.toArray(new Long[0])));
            ps.setObject(2, endAt);    // existingStart < newEnd
            ps.setObject(3, startAt);  // newStart < existingEnd

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (Exception e) {
            throw new RuntimeException("Error checking overlap for tableIds " + tableIds, e);
        }
    }

    /**
     * Non-transactional convenience overload for overlap checks.
     *
     * @param tableIds table IDs to check
     * @param startAt  start of requested window
     * @param endAt    end of requested window
     * @return true if any overlap exists
     */
    public boolean anyOverlappingForTables(
            List<Long> tableIds,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        try (Connection conn = Database.getConnection()) {
            return anyOverlappingForTables(conn, tableIds, startAt, endAt);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check overlapping reservations", e);
        }
    }

    /**
     * Returns which table IDs are blocked by overlapping reservations in the given window.
     *
     * <p>Transaction-aware: uses provided connection.</p>
     *
     * @param conn     existing database connection
     * @param tableIds candidate table IDs
     * @param startAt  requested start
     * @param endAt    requested end
     * @return set of overlapping/blocked table IDs (empty if none)
     */
    public Set<Long> findOverlappingTableIds(
            Connection conn,
            List<Long> tableIds,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        if (tableIds == null || tableIds.isEmpty()) {
            return Set.of();
        }

        String sql = """
            SELECT DISTINCT rt.table_id
            FROM reservation_tables rt
            JOIN reservations r ON r.reservation_id = rt.reservation_id
            WHERE rt.table_id = ANY (?::bigint[])
              AND r.status NOT IN ('CANCELLED', 'NO_SHOW')
              AND r.start_at < ?
              AND ? < COALESCE(r.end_at, r.start_at + interval '2 hours')
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setArray(1, conn.createArrayOf("bigint", tableIds.toArray(new Long[0])));
            ps.setObject(2, endAt);
            ps.setObject(3, startAt);

            Set<Long> blocked = new HashSet<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    blocked.add(rs.getLong("table_id"));
                }
            }

            return blocked;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find overlapping table ids", e);
        }
    }

    // -------------------------------------------------------------------------
    // Overlap queries (reservations for calendar UI)
    // -------------------------------------------------------------------------

    /**
     * Returns reservations that overlap a given time window.
     *
     * <p>This is UI-friendly for calendar views (day/week/month), where a reservation
     * should appear if it intersects the requested range.</p>
     *
     * <p>Overlap rule used:</p>
     * <ul>
     *   <li>ReservationStart &lt; WindowEnd</li>
     *   <li>WindowStart &lt; ReservationEnd</li>
     * </ul>
     *
     * <p>If {@code end_at} is null in an existing reservation, this query treats it as
     * {@code start_at + 2 hours} using {@code COALESCE}.</p>
     *
     * <p>Results are ordered by {@code start_at} ascending for predictable UI rendering.</p>
     *
     * <p>Non-transactional: obtains its own connection.</p>
     *
     * @param from start of the requested window (inclusive boundary by overlap rule)
     * @param to   end of the requested window (exclusive boundary by overlap rule)
     * @return list of reservation list items overlapping the window
     * @throws IllegalArgumentException if {@code from} or {@code to} is null
     * @throws RuntimeException if a database error occurs
     */
    public List<ReservationListItem> findReservationsOverlapping(
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }

        String sql = """
        SELECT
          r.reservation_id,
          r.customer_id,
          c.full_name,
          r.start_at,
          r.end_at,
          r.party_size,
          r.status,
          r.notes,
          r.created_at
        FROM reservations r
        JOIN customers c ON c.customer_id = r.customer_id
        WHERE r.start_at < ?
          AND ? < COALESCE(r.end_at, r.start_at + interval '2 hours')
        ORDER BY r.start_at
        """;

        List<ReservationListItem> results = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, to);
            ps.setObject(2, from);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapReservationListItem(rs));
                }
            }

            return results;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to list reservations overlapping window", e);
        }
    }

    /**
     * Returns reservations filtered by optional overlap window and optional status.
     *
     * <p>If {@code status} is provided, only reservations with that status are returned.</p>
     * <p>If {@code from} and {@code to} are provided, only reservations overlapping the window are returned.</p>
     *
     * <p>Overlap rule used:</p>
     * <ul>
     *   <li>ReservationStart &lt; WindowEnd</li>
     *   <li>WindowStart &lt; ReservationEnd</li>
     * </ul>
     *
     * <p>Results are ordered by {@code start_at} ascending for predictable UI rendering.</p>
     *
     * @param from   optional window start (must be paired with {@code to})
     * @param to     optional window end (must be paired with {@code from})
     * @param status optional reservation status filter
     * @return list of reservations matching the filters
     */
    public List<ReservationListItem> findFiltered(
            OffsetDateTime from,
            OffsetDateTime to,
            String status
    ) {
        StringBuilder sql = new StringBuilder("""
        SELECT
          r.reservation_id,
          r.customer_id,
          c.full_name,
          r.start_at,
          r.end_at,
          r.party_size,
          r.status,
          r.notes,
          r.created_at,
          COALESCE(
            ARRAY_AGG(t.table_code ORDER BY t.table_code)
              FILTER (WHERE t.table_code IS NOT NULL),
            ARRAY[]::text[]
          ) AS table_codes
        FROM reservations r
        JOIN customers c ON c.customer_id = r.customer_id
        LEFT JOIN reservation_tables rt ON rt.reservation_id = r.reservation_id
        LEFT JOIN tables t ON t.table_id = rt.table_id
        WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        // Window overlap filter (calendar)
        if (from != null && to != null) {
            sql.append("""
              AND r.start_at < ?
              AND ? < COALESCE(r.end_at, r.start_at + interval '2 hours')
            """);
            params.add(to);
            params.add(from);
        }

        // Status filter
        if (status != null && !status.isBlank()) {
            sql.append(" AND r.status = ? ");
            params.add(status.trim());
        }

        sql.append("""
         GROUP BY
          r.reservation_id,
          r.customer_id,
          c.full_name,
          r.start_at,
          r.end_at,
          r.party_size,
          r.status,
          r.notes,
          r.created_at
         ORDER BY r.start_at
        """);

        List<ReservationListItem> results = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapReservationListItem(rs));
                }
            }

            return results;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch filtered reservations", e);
        }
    }

    // -------------------------------------------------------------------------
    // Status transitions / updates
    // -------------------------------------------------------------------------

    /**
     * Cancels a reservation by setting status to {@code CANCELLED} and storing audit fields.
     *
     * <p>Idempotent behavior:</p>
     * <ul>
     *   <li>If {@code cancelled_at} already exists, it is not overwritten.</li>
     *   <li>If {@code cancellation_reason} already exists, it is not overwritten.</li>
     * </ul>
     *
     * <p>Only updates if the reservation is not already cancelled.</p>
     *
     * @param conn          existing database connection
     * @param reservationId reservation ID
     * @param reason        optional cancellation reason (nullable/blank allowed)
     * @return number of rows updated (0 or 1)
     */
    public int cancelById(Connection conn, long reservationId, String reason) {
        String sql = """
            UPDATE reservations
            SET status = 'CANCELLED',
                cancelled_at = COALESCE(cancelled_at, now()),
                cancellation_reason = COALESCE(cancellation_reason, ?)
            WHERE reservation_id = ?
              AND status <> 'CANCELLED'
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            String normalizedReason = (reason == null || reason.isBlank()) ? null : reason.trim();

            ps.setString(1, normalizedReason);
            ps.setLong(2, reservationId);

            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to cancel reservation id=" + reservationId, e);
        }
    }

    /**
     * Confirms a reservation.
     *
     * <p>Rule: only {@code PENDING -> CONFIRMED} is allowed.</p>
     *
     * @param conn          existing database connection
     * @param reservationId reservation ID
     * @return number of rows updated (0 or 1)
     */
    public int confirmById(Connection conn, long reservationId) {
        String sql = """
            UPDATE reservations
            SET status = 'CONFIRMED'
            WHERE reservation_id = ?
              AND status = 'PENDING'
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reservationId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to confirm reservation id=" + reservationId, e);
        }
    }

    /**
     * Marks a reservation as NO_SHOW.
     *
     * <p>Rule: only {@code PENDING -> NO_SHOW} or {@code CONFIRMED -> NO_SHOW} is allowed.</p>
     *
     * @param conn          existing database connection
     * @param reservationId reservation ID
     * @return number of rows updated (0 or 1)
     */
    public int noShowById(Connection conn, long reservationId) {
        String sql = """
        UPDATE reservations
        SET status = 'NO_SHOW'
        WHERE reservation_id = ?
          AND status IN ('PENDING', 'CONFIRMED')
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reservationId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark reservation as no-show id=" + reservationId, e);
        }
    }

    // -------------------------------------------------------------------------
    // Extra query: capacity information
    // -------------------------------------------------------------------------

    /**
     * DTO-like record for minimal capacity-related reservation information.
     *
     * @param partySize reservation party size
     * @param status    current reservation status
     */
    public record ReservationCapacityInfo(int partySize, String status) {}

    /**
     * Retrieves party size and status for a reservation.
     *
     * <p>Transaction-aware: uses provided connection.</p>
     *
     * @param conn          existing database connection
     * @param reservationId reservation ID
     * @return optional capacity info if reservation exists
     */
    public Optional<ReservationCapacityInfo> findCapacityInfoById(Connection conn, long reservationId) {
        String sql = """
            SELECT party_size, status
            FROM reservations
            WHERE reservation_id = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reservationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                return Optional.of(new ReservationCapacityInfo(
                        rs.getInt("party_size"),
                        rs.getString("status")
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to fetch reservation capacity info for id=" + reservationId, e
            );
        }
    }

    // -------------------------------------------------------------------------
    // Internal mapping helper
    // -------------------------------------------------------------------------

    private ReservationListItem mapReservationListItem(ResultSet rs) throws SQLException {

        Array tableArray = rs.getArray("table_codes");

        List<String> tableCodes = List.of();
        if (tableArray != null) {
            String[] values = (String[]) tableArray.getArray();
            tableCodes = (values == null) ? List.of() : Arrays.asList(values);
        }
        return new ReservationListItem(
                rs.getLong("reservation_id"),
                rs.getLong("customer_id"),
                rs.getString("full_name"),
                rs.getObject("start_at", OffsetDateTime.class),
                rs.getObject("end_at", OffsetDateTime.class),
                rs.getInt("party_size"),
                rs.getString("status"),
                rs.getString("notes"),
                rs.getObject("created_at", OffsetDateTime.class),
                tableCodes
        );
    }
}
