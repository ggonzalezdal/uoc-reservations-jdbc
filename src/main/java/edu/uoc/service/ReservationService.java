package edu.uoc.service;

import edu.uoc.dao.CustomerDao;
import edu.uoc.dao.ReservationDao;
import edu.uoc.dao.ReservationTableDao;
import edu.uoc.dao.TableDao;
import edu.uoc.db.Database;
import edu.uoc.model.Reservation;

import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

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
}
