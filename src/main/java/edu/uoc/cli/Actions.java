package edu.uoc.cli;

import edu.uoc.dao.CustomerDao;
import edu.uoc.dao.ReservationDao;
import edu.uoc.dao.ReservationTableDao;
import edu.uoc.dao.TableDao;
import edu.uoc.db.Database;
import edu.uoc.model.Customer;
import edu.uoc.model.Reservation;
import edu.uoc.service.ReservationService;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import static edu.uoc.cli.Input.*;

/**
 * CLI action handlers.
 *
 * <p>This class contains one method per menu option. Each method:</p>
 * <ul>
 *   <li>collects user input (via {@link Input})</li>
 *   <li>delegates to DAOs or Services</li>
 *   <li>prints results and errors to standard output</li>
 * </ul>
 *
 * <p>No business rules should be implemented here. Business rules belong in
 * the Service layer.</p>
 *
 * @since 1.0
 */
public final class Actions {

    private Actions() {
        // Prevent instantiation (utility class)
    }

    // -------------------------------------------------------------------------
    // Customers
    // -------------------------------------------------------------------------

    /**
     * Lists all customers in the system.
     *
     * @param dao customer DAO
     */
    public static void listCustomers(CustomerDao dao) {
        List<Customer> customers = dao.findAll();

        if (customers.isEmpty()) {
            System.out.println("No customers found.");
            return;
        }

        customers.forEach(System.out::println);
    }

    /**
     * Creates a new customer by collecting fields from the CLI.
     *
     * <p>Validation is intentionally minimal at CLI level to avoid obvious errors
     * before reaching the database layer.</p>
     *
     * @param dao customer DAO
     * @param sc  scanner for user input
     */
    public static void addCustomer(CustomerDao dao, Scanner sc) {
        System.out.print("Full name: ");
        String fullName = sc.nextLine().trim();

        if (fullName.isBlank()) {
            System.out.println("Full name cannot be empty.");
            return;
        }

        System.out.print("Phone (required): ");
        String phone = sc.nextLine().trim();

        if (phone.isBlank()) {
            System.out.println("Phone cannot be empty.");
            return;
        }

        System.out.print("Email (optional, press Enter to skip): ");
        String email = sc.nextLine().trim();
        if (email.isBlank()) {
            email = null;
        }

        long newId = dao.insert(fullName, phone, email);
        System.out.println("Customer inserted with id = " + newId);
    }

    /**
     * Finds a customer by its identifier and prints the result.
     *
     * @param dao customer DAO
     * @param sc  scanner for user input
     */
    public static void findCustomerById(CustomerDao dao, Scanner sc) {
        long id = readLong(sc, "Customer ID: ");

        Optional<Customer> result = dao.findById(id);
        result.ifPresentOrElse(
                System.out::println,
                () -> System.out.println("Customer not found.")
        );
    }

    // -------------------------------------------------------------------------
    // Reservations
    // -------------------------------------------------------------------------

    /**
     * Lists all reservations.
     *
     * @param dao reservation DAO
     */
    public static void listReservations(ReservationDao dao) {
        var reservations = dao.findAll();

        if (reservations.isEmpty()) {
            System.out.println("No reservations found.");
            return;
        }

        reservations.forEach(System.out::println);
    }

    /**
     * Creates a reservation using the Reservation DAO (direct insert).
     *
     * <p>This action is intentionally simple and bypasses table assignment logic.</p>
     *
     * @param dao reservation DAO
     * @param sc  scanner for user input
     */
    public static void addReservation(ReservationDao dao, Scanner sc) {
        System.out.println("=== Add reservation ===");

        long customerId = readLong(sc, "Customer ID: ");
        OffsetDateTime startAt = readDateTime(sc, "Start (yyyy-MM-dd HH:mm): ");

        System.out.print("End (optional, yyyy-MM-dd HH:mm, Enter to skip): ");
        String endInput = sc.nextLine().trim();
        OffsetDateTime endAt = null;

        if (!endInput.isBlank()) {
            endAt = parseDateTime(endInput);
            if (endAt == null) {
                System.out.println("Invalid end format. Use: yyyy-MM-dd HH:mm (example: 2026-02-21 22:30)");
                return;
            }
        }

        int partySize = readInt(sc, "Party size: ");

        System.out.print("Status [PENDING/CONFIRMED/CANCELLED/NO_SHOW] (Enter for PENDING): ");
        String statusInput = sc.nextLine().trim();
        String status = statusInput.isBlank() ? "PENDING" : statusInput.toUpperCase();

        if (!isValidStatus(status)) {
            System.out.println("Invalid status. Allowed: PENDING, CONFIRMED, CANCELLED, NO_SHOW.");
            return;
        }

        System.out.print("Notes (optional, Enter to skip): ");
        String notes = sc.nextLine().trim();
        if (notes.isBlank()) notes = null;

        Reservation r = new Reservation(customerId, startAt, endAt, partySize, status, notes);
        long id = dao.insert(r);

        System.out.println("Reservation inserted with id = " + id);
    }

    // -------------------------------------------------------------------------
    // Tables
    // -------------------------------------------------------------------------

    /**
     * Lists all restaurant tables.
     *
     * @param dao table DAO
     */
    public static void listTables(TableDao dao) {
        var tables = dao.findAll();

        if (tables.isEmpty()) {
            System.out.println("No tables found.");
            return;
        }

        tables.forEach(System.out::println);
    }

    // -------------------------------------------------------------------------
    // Assignments / Availability
    // -------------------------------------------------------------------------

    /**
     * Displays the table IDs assigned to a reservation.
     *
     * @param dao reservation-table DAO
     * @param sc  scanner for user input
     */
    public static void showReservationAssignments(ReservationTableDao dao, Scanner sc) {
        long reservationId = readLong(sc, "Reservation ID: ");
        var tableIds = dao.findTableIdsByReservationId(reservationId);

        if (tableIds.isEmpty()) {
            System.out.println("No tables assigned to this reservation.");
            return;
        }

        System.out.println("Assigned table_ids: " + tableIds);
    }

    /**
     * Assigns tables to an existing reservation using the Service layer validation.
     *
     * @param service reservation service
     * @param tableDao table DAO (used for resolving table codes)
     * @param sc scanner
     */
    public static void assignTablesToReservation(ReservationService service, TableDao tableDao, Scanner sc) {
        long reservationId = readExistingReservationIdUntilValid(sc, new ReservationDao(), "Reservation ID: ");

        List<Long> tableIds = readTableIdsUntilValid(
                sc,
                tableDao,
                "Table codes (comma-separated, e.g. T1,T4): "
        );

        try {
            service.assignTablesToReservationValidated(reservationId, tableIds);
            System.out.println("[OK] Assigned " + tableIds.size() + " table(s) to reservation " + reservationId + ".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("[ERROR] Cannot assign tables: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Creates a reservation and assigns tables in a single transaction.
     *
     * <p>Delegates to {@link ReservationService#createReservationWithTables(long, OffsetDateTime, OffsetDateTime, int, String, String, List)}.</p>
     *
     * @param service reservation service
     * @param tableDao table DAO
     * @param sc scanner
     */
    public static void createReservationWithTables(ReservationService service, TableDao tableDao, Scanner sc) {
        System.out.println("=== Create reservation + assign tables (transaction-safe) ===");

        long customerId = readLong(sc, "Customer ID: ");
        if (!customerExistsOrAbort(new CustomerDao(), customerId)) return;

        OffsetDateTime startAt = readDateTime(sc, "Start (yyyy-MM-dd HH:mm): ");

        OffsetDateTime endAt = readOptionalEndDateTime(
                sc,
                "End (optional, yyyy-MM-dd HH:mm, Enter to skip -> defaults to +2 hours): "
        );

        int partySize = readInt(sc, "Party size: ");

        String status = readValidStatusOrDefault(
                sc,
                "Status [PENDING/CONFIRMED/CANCELLED/NO_SHOW] (Enter for PENDING): ",
                "PENDING"
        );

        System.out.print("Notes (optional, Enter to skip): ");
        String notes = sc.nextLine().trim();
        if (notes.isBlank()) notes = null;

        List<Long> tableIds = readTableIdsUntilValid(
                sc,
                tableDao,
                "Table codes (comma-separated, e.g. T1,T4): "
        );

        try {
            long reservationId = service.createReservationWithTables(
                    customerId,
                    startAt,
                    endAt,
                    partySize,
                    status,
                    notes,
                    tableIds
            );

            System.out.println("[OK] Reservation created with id = " + reservationId);
            System.out.println("[OK] Assigned " + tableIds.size() + " table(s).");

        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("[ERROR] Cannot create reservation: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Lists tables available within a time window.
     *
     * @param service reservation service
     * @param sc scanner
     */
    public static void listAvailableTables(ReservationService service, Scanner sc) {
        System.out.println("=== List available tables ===");

        OffsetDateTime startAt = readDateTime(sc, "Start (yyyy-MM-dd HH:mm): ");

        OffsetDateTime endAt = readOptionalEndDateTime(
                sc,
                "End (optional, yyyy-MM-dd HH:mm, Enter to skip -> defaults to +2 hours): "
        );

        try {
            var availableTables = service.listAvailableTables(startAt, endAt);

            if (availableTables.isEmpty()) {
                System.out.println("No available tables for that time window.");
                return;
            }

            System.out.println("Available tables:");
            availableTables.forEach(System.out::println);

        } catch (IllegalArgumentException e) {
            System.out.println("[ERROR] " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Cancels a reservation.
     *
     * @param service reservation service
     * @param sc scanner
     */
    public static void cancelReservation(ReservationService service, Scanner sc) {
        System.out.println("=== Cancel reservation ===");

        long reservationId = readExistingReservationIdUntilValid(sc, new ReservationDao(), "Reservation ID: ");

        System.out.print("Cancellation reason (optional, Enter to skip): ");
        String reason = sc.nextLine().trim();
        if (reason.isBlank()) reason = null;

        try {
            boolean cancelledNow = service.cancelReservation(reservationId, reason);

            if (cancelledNow) {
                System.out.println("[OK] Reservation cancelled: " + reservationId);
            } else {
                System.out.println("[OK] Reservation was already cancelled: " + reservationId);
            }

        } catch (IllegalArgumentException e) {
            System.out.println("[ERROR] " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Confirms a reservation.
     *
     * @param service reservation service
     * @param sc scanner
     */
    public static void confirmReservation(ReservationService service, Scanner sc) {
        System.out.println("=== Confirm reservation ===");

        long reservationId = readExistingReservationIdUntilValid(sc, new ReservationDao(), "Reservation ID: ");

        try {
            boolean confirmedNow = service.confirmReservation(reservationId);

            if (confirmedNow) {
                System.out.println("[OK] Reservation confirmed: " + reservationId);
            } else {
                System.out.println("[OK] Reservation was already confirmed: " + reservationId);
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("[ERROR] " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Creates a reservation and auto-assigns tables using a greedy strategy.
     *
     * @param service reservation service
     * @param sc scanner
     */
    public static void createReservationAutoAssign(ReservationService service, Scanner sc) {
        System.out.println("=== Create reservation + auto-assign tables (greedy) ===");

        long customerId = readLong(sc, "Customer ID: ");
        if (!customerExistsOrAbort(new CustomerDao(), customerId)) return;

        OffsetDateTime startAt = readDateTime(sc, "Start (yyyy-MM-dd HH:mm): ");

        OffsetDateTime endAt = readOptionalEndDateTime(
                sc,
                "End (optional, yyyy-MM-dd HH:mm, Enter to skip -> defaults to +2 hours): "
        );

        int partySize = readInt(sc, "Party size: ");

        String status = readValidStatusOrDefault(
                sc,
                "Status [PENDING/CONFIRMED/CANCELLED/NO_SHOW] (Enter for PENDING): ",
                "PENDING"
        );

        System.out.print("Notes (optional, Enter to skip): ");
        String notes = sc.nextLine().trim();
        if (notes.isBlank()) notes = null;

        try {
            long reservationId = service.createReservationAutoAssignTables(
                    customerId,
                    startAt,
                    endAt,
                    partySize,
                    status,
                    notes
            );

            System.out.println("[OK] Reservation created with id = " + reservationId);
            System.out.println("[OK] Tables auto-assigned (greedy).");

        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("[ERROR] Cannot create reservation: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[ERROR] Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Activates a restaurant table.
     *
     * @param tableDao table DAO
     * @param sc scanner
     */
    public static void activateTable(TableDao tableDao, Scanner sc) {
        System.out.println("=== Activate table ===");

        long tableId = readExistingTableIdUntilValid(sc, tableDao, "Table ID: ");

        try {
            boolean updated = tableDao.setActive(tableId, true);
            if (updated) {
                System.out.println("[OK] Table activated: " + tableId);
            } else {
                System.out.println("[ERROR] Could not activate table: " + tableId);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Deactivates a restaurant table.
     *
     * @param tableDao table DAO
     * @param sc scanner
     */
    public static void deactivateTable(TableDao tableDao, Scanner sc) {
        System.out.println("=== Deactivate table ===");

        long tableId = readExistingTableIdUntilValid(sc, tableDao, "Table ID: ");

        try {
            boolean updated = tableDao.setActive(tableId, false);
            if (updated) {
                System.out.println("[OK] Table deactivated: " + tableId);
            } else {
                System.out.println("[ERROR] Could not deactivate table: " + tableId);
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Unexpected error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Internal validation helper (CLI-level)
    // -------------------------------------------------------------------------

    /**
     * Checks whether a customer exists and prints an error message if not.
     *
     * <p>This is a CLI convenience helper to prevent obvious failures before
     * invoking Service operations. It performs a read-only DB check.</p>
     *
     * @param customerDao customer DAO
     * @param customerId customer ID to validate
     * @return true if customer exists, false otherwise
     */
    private static boolean customerExistsOrAbort(CustomerDao customerDao, long customerId) {
        try (Connection conn = Database.getConnection()) {
            if (!customerDao.existsById(conn, customerId)) {
                System.out.println("[ERROR] Customer not found: " + customerId);
                return false;
            }
            return true;
        } catch (SQLException e) {
            System.out.println("[ERROR] DB error while checking customer: " + e.getMessage());
            return false;
        }
    }
}
