package edu.uoc.cli;

/**
 * Renders the interactive CLI menu.
 *
 * <p>This class is intentionally limited to presentation concerns.
 * It does not contain business logic or input handling.</p>
 *
 * <p>Separating menu rendering from control flow improves:</p>
 * <ul>
 *     <li>Maintainability</li>
 *     <li>Readability</li>
 *     <li>Future UI extensibility (colors, formatting, pagination)</li>
 * </ul>
 *
 * @since 1.0
 */
public final class MenuPrinter {

    private MenuPrinter() {
        // Utility class - prevent instantiation
    }

    /**
     * Prints the main menu to standard output.
     *
     * <p>The numbering must stay aligned with
     * {@link MenuApp#run()} switch cases.</p>
     */
    public static void printMenu() {
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

        System.out.println("Tables");
        System.out.println("6) List tables");
        System.out.println();

        System.out.println("Assignments");
        System.out.println("7) Show table assignments for reservation");
        System.out.println("8) Assign tables to reservation");
        System.out.println("9) Create reservation + assign tables (transaction-safe)");
        System.out.println("10) List available tables for a time window");
        System.out.println("11) Cancel reservation");
        System.out.println("12) Confirm reservation");
        System.out.println("13) Create reservation + auto-assign tables (greedy)");
        System.out.println("14) Activate table");
        System.out.println("15) Deactivate table");

        System.out.println();
        System.out.println("0) Exit");
    }
}
