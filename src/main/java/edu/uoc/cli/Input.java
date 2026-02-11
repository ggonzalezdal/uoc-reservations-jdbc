package edu.uoc.cli;

import edu.uoc.dao.ReservationDao;
import edu.uoc.dao.TableDao;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Utility class for CLI input handling and validation.
 *
 * <p>This class centralizes all user input parsing, re-prompt logic,
 * and lightweight validation used by the CLI layer.</p>
 *
 * <p>Design principles:</p>
 * <ul>
 *   <li>No business logic</li>
 *   <li>No database writes</li>
 *   <li>Re-prompts until valid input is provided</li>
 * </ul>
 *
 * @since 1.0
 */
public final class Input {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private Input() {
        // Prevent instantiation (utility class)
    }

    /**
     * Reads an integer from standard input.
     * Re-prompts until a valid integer is entered.
     *
     * @param sc     scanner used for input
     * @param prompt prompt displayed to the user
     * @return parsed integer value
     */
    public static int readInt(Scanner sc, String prompt) {
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

    /**
     * Reads a long value from standard input.
     * Re-prompts until valid.
     *
     * @param sc     scanner used for input
     * @param prompt prompt displayed to the user
     * @return parsed long value
     */
    public static long readLong(Scanner sc, String prompt) {
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

    /**
     * Reads a date-time value in format {@code yyyy-MM-dd HH:mm}.
     *
     * <p>Converts the value to {@link OffsetDateTime}
     * using the system default time zone.</p>
     *
     * <p>Re-prompts until valid format is entered.</p>
     *
     * @param sc     scanner used for input
     * @param prompt prompt displayed to the user
     * @return parsed {@link OffsetDateTime}
     */
    public static OffsetDateTime readDateTime(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();

            try {
                LocalDateTime ldt = LocalDateTime.parse(input, DATE_TIME_FORMAT);
                return ldt.atZone(SYSTEM_ZONE).toOffsetDateTime();
            } catch (DateTimeParseException e) {
                System.out.println("Invalid format. Use: yyyy-MM-dd HH:mm (example: 2026-02-21 20:30)");
            }
        }
    }

    /**
     * Parses a date-time string using the standard CLI format.
     *
     * @param input date-time string
     * @return parsed {@link OffsetDateTime}, or {@code null} if invalid
     */
    public static OffsetDateTime parseDateTime(String input) {
        try {
            LocalDateTime ldt = LocalDateTime.parse(input, DATE_TIME_FORMAT);
            return ldt.atZone(SYSTEM_ZONE).toOffsetDateTime();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Reads an optional end date-time.
     *
     * <p>If the user presses Enter, {@code null} is returned.
     * Otherwise, re-prompts until valid format is entered.</p>
     *
     * @param sc     scanner used for input
     * @param prompt prompt displayed to the user
     * @return parsed {@link OffsetDateTime} or {@code null}
     */
    public static OffsetDateTime readOptionalEndDateTime(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();

            if (input.isBlank()) {
                return null;
            }

            OffsetDateTime parsed = parseDateTime(input);
            if (parsed != null) {
                return parsed;
            }

            System.out.println("Invalid format. Use: yyyy-MM-dd HH:mm (example: 2026-02-21 22:30)");
        }
    }

    /**
     * Validates reservation status value.
     *
     * @param status reservation status
     * @return true if allowed
     */
    public static boolean isValidStatus(String status) {
        return status.equals("PENDING")
                || status.equals("CONFIRMED")
                || status.equals("CANCELLED")
                || status.equals("NO_SHOW");
    }

    /**
     * Reads reservation status.
     * Returns default if blank.
     *
     * @param sc            scanner used for input
     * @param prompt        prompt displayed to user
     * @param defaultStatus fallback value if blank
     * @return validated status
     */
    public static String readValidStatusOrDefault(
            Scanner sc,
            String prompt,
            String defaultStatus
    ) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();

            if (input.isBlank()) {
                return defaultStatus;
            }

            String status = input.toUpperCase();
            if (isValidStatus(status)) {
                return status;
            }

            System.out.println("Invalid status. Allowed: PENDING, CONFIRMED, CANCELLED, NO_SHOW.");
        }
    }

    /**
     * Reads comma-separated table codes and resolves them to IDs.
     *
     * <p>Re-prompts until all provided codes exist in the database.</p>
     *
     * @param sc       scanner
     * @param tableDao table DAO used for resolution
     * @param prompt   input prompt
     * @return list of table IDs
     */
    public static List<Long> readTableIdsUntilValid(
            Scanner sc,
            TableDao tableDao,
            String prompt
    ) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();

            List<Long> ids = resolveTableIdsOrNull(tableDao, input);
            if (ids != null) {
                return ids;
            }
        }
    }

    /**
     * Reads a reservation ID and ensures it exists.
     *
     * @param sc             scanner
     * @param reservationDao DAO used for validation
     * @param prompt         input prompt
     * @return valid reservation ID
     */
    public static long readExistingReservationIdUntilValid(
            Scanner sc,
            ReservationDao reservationDao,
            String prompt
    ) {
        while (true) {
            long id = readLong(sc, prompt);

            if (reservationDao.existsById(id)) {
                return id;
            }

            System.out.println("Reservation not found: " + id);
        }
    }

    /**
     * Reads a table ID and ensures it exists.
     *
     * @param sc       scanner
     * @param tableDao DAO used for validation
     * @param prompt   input prompt
     * @return valid table ID
     */
    public static long readExistingTableIdUntilValid(
            Scanner sc,
            TableDao tableDao,
            String prompt
    ) {
        while (true) {
            long id = readLong(sc, prompt);

            if (tableDao.existsById(id)) {
                return id;
            }

            System.out.println("Table not found: " + id);
        }
    }

    // ---------------------------
    // Private helper
    // ---------------------------

    private static List<Long> resolveTableIdsOrNull(TableDao tableDao, String csv) {
        if (csv == null || csv.isBlank()) {
            System.out.println("No table codes provided.");
            return null;
        }

        String[] parts = csv.split(",");
        List<Long> ids = new ArrayList<>();

        for (String part : parts) {
            String code = part.trim().toUpperCase();
            if (code.isBlank()) continue;

            var tableOpt = tableDao.findByCode(code);
            if (tableOpt.isEmpty()) {
                System.out.println("Unknown table code: " + code);
                return null;
            }

            ids.add(tableOpt.get().getId());
        }

        if (ids.isEmpty()) {
            System.out.println("No valid table codes provided.");
            return null;
        }

        return ids;
    }
}

