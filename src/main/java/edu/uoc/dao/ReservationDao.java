package edu.uoc.dao;

import edu.uoc.db.Database;
import edu.uoc.dto.ReservationListItem;
import edu.uoc.model.Reservation;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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
              r.party_size,
              r.status
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
                int partySize = rs.getInt("party_size");
                String status = rs.getString("status");

                results.add(new ReservationListItem(
                        reservationId,
                        customerId,
                        customerName,
                        startAt,
                        partySize,
                        status
                ));
            }

            return results;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations", e);
        }
    }

    /**
     * Inserts a reservation and returns the generated reservation_id.
     * Also sets the generated id back into the Reservation object.
     */
    public long insert(Reservation r) {
        String sql = """
            INSERT INTO reservations (customer_id, start_at, party_size, status)
            VALUES (?, ?, ?, ?)
            RETURNING reservation_id
            """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, r.getCustomerId());
            ps.setObject(2, r.getStartAt());         // OffsetDateTime -> timestamptz
            ps.setInt(3, r.getPartySize());
            ps.setString(4, r.getStatus());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Insert succeeded but no reservation_id returned.");
                }
                long id = rs.getLong("reservation_id");
                r.setReservationId(id);
                return id;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }
    }
}

