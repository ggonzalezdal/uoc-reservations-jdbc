package edu.uoc;

import edu.uoc.dao.CustomerDao;
import edu.uoc.dao.ReservationDao;
import edu.uoc.db.Database;
import edu.uoc.model.Customer;
import edu.uoc.model.Reservation;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        // Choose ONE mode while learning:
        //runConnectionCheck();
        //runDaoDemo();
        runMenu();

        // Quick test (optional)
        //ReservationDao reservationDao = new ReservationDao();
        //reservationDao.findAll().forEach(System.out::println);
    }

    /**
     * JDBC connection diagnostics.
     * Verifies env vars, driver, and DB availability.
     */
    private static void runConnectionCheck() {
        try (Connection conn = Database.getConnection()) {

            System.out.println("Connected to database!");
            System.out.println("DB product   : " + conn.getMetaData().getDatabaseProductName());
            System.out.println("DB version   : " + conn.getMetaData().getDatabaseProductVersion());
            System.out.println("JDBC driver  : " + conn.getMetaData().getDriverName());
            System.out.println("Driver ver.  : " + conn.getMetaData().getDriverVersion());

        } catch (SQLException e) {
            System.err.println("Connection failed!");
            e.printStackTrace();
        }
    }

    /**
     * DAO playground.
     * Preserves all CRUD experiments in executable form.
     */
    private static void runDaoDemo() {
        CustomerDao dao = new CustomerDao();

        // INSERT demo
        long id = dao.insert("Gordon Ramsay", "gordon@kitchen.com");
        System.out.println("Inserted customer with id = " + id);

        // FIND ALL demo
        System.out.println("\nAll customers:");
        dao.findAll().forEach(System.out::println);

        // FIND BY ID demo (Optional pattern A)
        System.out.println("\nFind customer by id:");
        dao.findById(id)
                .ifPresentOrElse(
                        System.out::println,
                        () -> System.out.println("Customer not found")
                );
    }

    /**
     * Main application flow.
     * Simple CLI menu using DAO methods.
     */
    private static void runMenu() {
        CustomerDao customerDao = new CustomerDao();
        ReservationDao reservationDao = new ReservationDao();
        Scanner sc = new Scanner(System.in);

        while (true) {
            printMenu();
            int option = readInt(sc, "Choose an option: ");

            switch (option) {
                case 1 -> listCustomers(customerDao);
                case 2 -> addCustomer(customerDao, sc);
                case 3 -> findCustomerById(customerDao, sc);

                case 4 -> listReservations(reservationDao);
                case 5 -> addReservation(reservationDao, sc);

                case 0 -> {
                    System.out.println("Bye!");
                    return;
                }
                default -> System.out.println("Invalid option. Try again.");
            }

            System.out.println();
        }
    }

    // ===== Menu helpers =====

    private static void printMenu() {
        System.out.println("=== Main Menu ===");
        System.out.println("Customers");
        System.out.println("1) List customers");
        System.out.println("2) Add customer");
        System.out.println("3) Find customer by ID");
        System.out.println();
        System.out.println("Reservations");
        System.out.println("4) List reservations");
        System.out.println("5) Add reservation");
        System.out.println();
        System.out.println("0) Exit");
    }

    private static void listCustomers(CustomerDao dao) {
        List<Customer> customers = dao.findAll();

        if (customers.isEmpty()) {
            System.out.println("No customers found.");
            return;
        }

        customers.forEach(System.out::println);
    }

    private static void addCustomer(CustomerDao dao, Scanner sc) {
        System.out.print("Full name: ");
        String fullName = sc.nextLine().trim();

        if (fullName.isBlank()) {
            System.out.println("Full name cannot be empty.");
            return;
        }

        System.out.print("Email (optional, press Enter to skip): ");
        String email = sc.nextLine().trim();
        if (email.isBlank()) {
            email = null;
        }

        long newId = dao.insert(fullName, email);
        System.out.println("Customer inserted with id = " + newId);
    }

    private static void findCustomerById(CustomerDao dao, Scanner sc) {
        long id = readLong(sc, "Customer ID: ");

        Optional<Customer> result = dao.findById(id);
        result.ifPresentOrElse(
                System.out::println,
                () -> System.out.println("Customer not found.")
        );
    }

    private static void listReservations(ReservationDao dao) {
        var reservations = dao.findAll();

        if (reservations.isEmpty()) {
            System.out.println("No reservations found.");
            return;
        }

        reservations.forEach(System.out::println);
    }

    private static void addReservation(ReservationDao dao, Scanner sc) {
        System.out.println("=== Add reservation ===");

        long customerId = readLong(sc, "Customer ID: ");
        OffsetDateTime startAt = readDateTime(sc, "Start (yyyy-MM-dd HH:mm): ");
        int partySize = readInt(sc, "Party size: ");

        System.out.print("Status [PENDING/CONFIRMED/CANCELLED] (Enter for PENDING): ");
        String statusInput = sc.nextLine().trim();
        String status = statusInput.isBlank() ? "PENDING" : statusInput.toUpperCase();

        Reservation r = new Reservation(customerId, startAt, partySize, status);
        long id = dao.insert(r);

        System.out.println("Reservation inserted with id = " + id);
    }

    // ===== Input helpers =====

    private static int readInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private static long readLong(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static OffsetDateTime readDateTime(Scanner sc, String prompt) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        ZoneId zone = ZoneId.systemDefault();

        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();

            try {
                LocalDateTime ldt = LocalDateTime.parse(input, fmt);
                return ldt.atZone(zone).toOffsetDateTime();
            } catch (DateTimeParseException e) {
                System.out.println("Invalid format. Use: yyyy-MM-dd HH:mm (example: 2026-02-21 20:30)");
            }
        }
    }
}
