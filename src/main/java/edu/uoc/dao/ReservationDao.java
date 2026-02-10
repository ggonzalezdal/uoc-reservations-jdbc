package edu.uoc.dao;

import edu.uoc.db.Database;
import edu.uoc.dto.ReservationListItem;
import edu.uoc.model.Reservation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;

public class ReservationDao {

    /**
     * Returns reservations joined with customer name, ordered by start time.
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
                long reservationId = rs.getLong("reservation_id");
                long customerId = rs.getLong("customer_id");
                String customerName = rs.getString("full_name");
                OffsetDateTime startAt = rs.getObject("start_at", OffsetDateTime.class);
                OffsetDateTime endAt = rs.getObject("end_at", OffsetDateTime.class); // nullable
                int partySize = rs.getInt("party_size");
                String status = rs.getString("status");
                String notes = rs.getString("notes"); // nullable
                OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);

                results.add(new ReservationListItem(
                        reservationId,
                        customerId,
                        customerName,
                        startAt,
                        endAt,
                        partySize,
                        status,
                        notes,
                        createdAt
                ));
            }

            return results;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations", e);
        }
    }

    /**
     * Inserts a reservation using the provided Connection (transaction-aware).
     * Returns the generated reservation_id and sets reservationId + createdAt into the Reservation object.
     *
     * Note: This method does NOT commit/rollback. The caller controls the transaction.
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
            ps.setObject(3, r.getEndAt());     // nullable ok
            ps.setInt(4, r.getPartySize());
            ps.setString(5, r.getStatus());
            ps.setString(6, r.getNotes());     // nullable ok

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
     * Backwards-compatible convenience method (non-transactional).
     * Opens its own connection.
     */
    public long insert(Reservation r) {
        try (Connection conn = Database.getConnection()) {
            return insert(conn, r);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }
    }

    public boolean anyOverlappingForTables(Connection conn,
                                           List<Long> tableIds,
                                           OffsetDateTime startAt,
                                           OffsetDateTime endAt) {

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

            // tableIds -> BIGINT[] parameter
            ps.setArray(1, conn.createArrayOf("bigint", tableIds.toArray(new Long[0])));

            // overlap conditions
            ps.setObject(2, endAt);    // existingStart < newEnd
            ps.setObject(3, startAt);  // newStart < existingEnd

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // if any row found => overlap exists
            }

        } catch (Exception e) {
            throw new RuntimeException("Error checking overlap for tableIds " + tableIds, e);
        }
    }

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

    /**
     * Returns the current status for a reservation, or empty if not found.
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

    /**
     * Cancels a reservation (status=CANCELLED) and stores audit fields.
     *
     * Idempotent rule:
     * - If cancelled_at already exists, do NOT overwrite it.
     * - If cancellation_reason already exists, do NOT overwrite it.
     *
     * @return rows updated (0 or 1)
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

            // Allow NULL if reason is blank/not provided
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
     * Rule: only PENDING -> CONFIRMED is allowed.
     *
     * @return rows updated (0 or 1)
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

    public record ReservationCapacityInfo(int partySize, String status) {}

    public Optional<ReservationCapacityInfo> findCapacityInfoById(
            Connection conn,
            long reservationId
    ) {
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

}
