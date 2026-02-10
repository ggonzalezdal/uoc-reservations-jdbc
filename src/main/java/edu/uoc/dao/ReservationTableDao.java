package edu.uoc.dao;

import edu.uoc.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ReservationTableDao {

    /**
     * Assigns ONE table to ONE reservation.
     * Uses ON CONFLICT DO NOTHING because (reservation_id, table_id) is the PK.
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
            throw new RuntimeException("Error assigning table " + tableId + " to reservation " + reservationId, e);
        }
    }

    /**
     * Assigns MANY tables to ONE reservation using batch insert.
     * Transaction-safe: caller controls commit/rollback via the Connection.
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
     * Convenience method (non-transactional): opens its own connection.
     * Useful for demos/CLI until we introduce the service layer.
     */
    public void addAssignments(long reservationId, List<Long> tableIds) {
        try (Connection conn = Database.getConnection()) {
            addAssignments(conn, reservationId, tableIds);
        } catch (Exception e) {
            throw new RuntimeException("Error assigning tables to reservation " + reservationId, e);
        }
    }

    /**
     * Returns table_ids assigned to a reservation (raw ids).
     * Useful for verification and later service logic.
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
            throw new RuntimeException("Error fetching table assignments for reservation " + reservationId, e);
        }

        return tableIds;
    }

    public void deleteAssignments(Connection conn, long reservationId) {
        String sql = "DELETE FROM reservation_tables WHERE reservation_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reservationId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting assignments for reservation " + reservationId, e);
        }
    }

    public void replaceAssignments(Connection conn, long reservationId, List<Long> tableIds) {
        deleteAssignments(conn, reservationId);
        addAssignments(conn, reservationId, tableIds);
    }

}
