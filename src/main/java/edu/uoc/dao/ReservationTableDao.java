package edu.uoc.dao;

import edu.uoc.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for reservation-table assignments.
 *
 * <p>Manages the join table {@code reservation_tables} which represents
 * a many-to-many relationship between reservations and physical restaurant tables.</p>
 *
 * <p>Database assumptions:</p>
 * <ul>
 *   <li>The primary key is {@code (reservation_id, table_id)}.</li>
 *   <li>Inserts use {@code ON CONFLICT DO NOTHING} to keep operations idempotent.</li>
 * </ul>
 *
 * <p>Transaction-aware methods accept a {@link Connection} and do not commit/rollback.
 * Convenience overloads open their own connection for simple CLI usage.</p>
 *
 * @since 1.0
 */
public class ReservationTableDao {

    /**
     * Assigns one table to one reservation.
     *
     * <p>Idempotent insert: if the assignment already exists, it is ignored
     * due to {@code ON CONFLICT DO NOTHING}.</p>
     *
     * <p>Transaction-aware: uses provided connection; caller controls commit/rollback.</p>
     *
     * @param conn          existing database connection
     * @param reservationId reservation ID
     * @param tableId       table ID
     * @throws RuntimeException if a database error occurs
     */
    public void addAssignment(Connection conn, long reservationId, long tableId) {
        String sql = """
            INSERT INTO reservation_tables (reservation_id, table_id)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reservationId);
            ps.setLong(2, tableId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error assigning table " + tableId + " to reservation " + reservationId,
                    e
            );
        }
    }

    /**
     * Assigns multiple tables to one reservation using JDBC batch insert.
     *
     * <p>Idempotent: duplicate assignments are ignored.</p>
     *
     * <p>Transaction-aware: uses provided connection; caller controls commit/rollback.</p>
     *
     * @param conn          existing database connection
     * @param reservationId reservation ID
     * @param tableIds      list of table IDs to assign
     * @throws RuntimeException if a database error occurs
     */
    public void addAssignments(Connection conn, long reservationId, List<Long> tableIds) {
        String sql = """
            INSERT INTO reservation_tables (reservation_id, table_id)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Long tableId : tableIds) {
                ps.setLong(1, reservationId);
                ps.setLong(2, tableId);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            throw new RuntimeException("Error assigning tables to reservation " + reservationId, e);
        }
    }

    /**
     * Convenience overload for assigning multiple tables without an explicit transaction.
     *
     * <p>Opens a connection internally and delegates to
     * {@link #addAssignments(Connection, long, List)}.</p>
     *
     * @param reservationId reservation ID
     * @param tableIds      list of table IDs to assign
     * @throws RuntimeException if a database error occurs
     */
    public void addAssignments(long reservationId, List<Long> tableIds) {
        try (Connection conn = Database.getConnection()) {
            addAssignments(conn, reservationId, tableIds);
        } catch (Exception e) {
            throw new RuntimeException("Error assigning tables to reservation " + reservationId, e);
        }
    }

    /**
     * Retrieves all table IDs assigned to a reservation.
     *
     * <p>Returns raw IDs, ordered ascending. This method is commonly used by the CLI
     * and by service validation logic.</p>
     *
     * @param reservationId reservation ID
     * @return list of assigned table IDs (empty if none)
     * @throws RuntimeException if a database error occurs
     */
    public List<Long> findTableIdsByReservationId(long reservationId) {
        String sql = """
            SELECT table_id
            FROM reservation_tables
            WHERE reservation_id = ?
            ORDER BY table_id
            """;

        List<Long> tableIds = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, reservationId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableIds.add(rs.getLong("table_id"));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error fetching table assignments for reservation " + reservationId,
                    e
            );
        }

        return tableIds;
    }

    /**
     * Deletes all assignments for a reservation.
     *
     * <p>Transaction-aware: uses provided connection; caller controls commit/rollback.</p>
     *
     * @param conn          existing database connection
     * @param reservationId reservation ID
     * @throws RuntimeException if a database error occurs
     */
    public void deleteAssignments(Connection conn, long reservationId) {
        String sql = "DELETE FROM reservation_tables WHERE reservation_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reservationId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting assignments for reservation " + reservationId, e);
        }
    }

    /**
     * Replaces the set of assigned tables for a reservation.
     *
     * <p>Implementation strategy:</p>
     * <ol>
     *   <li>Delete existing assignments</li>
     *   <li>Insert the new assignments (batch)</li>
     * </ol>
     *
     * <p>Transaction-aware: must be executed inside a transaction to avoid leaving
     * the reservation temporarily without assignments if a failure occurs.</p>
     *
     * @param conn          existing database connection
     * @param reservationId reservation ID
     * @param tableIds      new set of table IDs to assign
     * @throws RuntimeException if a database error occurs
     */
    public void replaceAssignments(Connection conn, long reservationId, List<Long> tableIds) {
        deleteAssignments(conn, reservationId);
        addAssignments(conn, reservationId, tableIds);
    }
}
