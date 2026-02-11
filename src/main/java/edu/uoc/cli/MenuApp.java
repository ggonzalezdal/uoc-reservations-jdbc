package edu.uoc.cli;

import edu.uoc.dao.CustomerDao;
import edu.uoc.dao.ReservationDao;
import edu.uoc.dao.ReservationTableDao;
import edu.uoc.dao.TableDao;
import edu.uoc.service.ReservationService;

import java.util.Scanner;

import static edu.uoc.Main.*;
import static edu.uoc.cli.Actions.*;
import static edu.uoc.cli.Input.readInt;

/**
 * Main CLI controller.
 *
 * <p>This class wires together DAOs, Services and user input.
 * It runs the interactive menu loop until the user exits.</p>
 *
 * <p>This class does not implement business rules.
 * All validation and transactional logic belongs in the Service layer.</p>
 *
 * @since 1.0
 */
public class MenuApp {

    /**
     * Starts the interactive menu loop.
     *
     * <p>The method continuously prompts the user for actions
     * until option 0 (Exit) is selected.</p>
     */
    public void run() {

        CustomerDao customerDao = new CustomerDao();
        ReservationDao reservationDao = new ReservationDao();
        TableDao tableDao = new TableDao();
        ReservationTableDao reservationTableDao = new ReservationTableDao();
        ReservationService reservationService = new ReservationService();

        Scanner sc = new Scanner(System.in);

        while (true) {
            MenuPrinter.printMenu();
            int option = readInt(sc, "Choose an option: ");

            switch (option) {
                case 1 -> listCustomers(customerDao);
                case 2 -> addCustomer(customerDao, sc);
                case 3 -> findCustomerById(customerDao, sc);

                case 4 -> listReservations(reservationDao);
                case 5 -> addReservation(reservationDao, sc);

                case 6 -> listTables(tableDao);
                case 7 -> showReservationAssignments(reservationTableDao, sc);

                case 8 -> assignTablesToReservation(reservationService, tableDao, sc);
                case 9 -> createReservationWithTables(reservationService, tableDao, sc);
                case 10 -> listAvailableTables(reservationService, sc);
                case 11 -> cancelReservation(reservationService, sc);
                case 12 -> confirmReservation(reservationService, sc);
                case 13 -> createReservationAutoAssign(reservationService, sc);

                case 14 -> activateTable(tableDao, sc);
                case 15 -> deactivateTable(tableDao, sc);

                case 0 -> {
                    System.out.println("Bye!");
                    return;
                }
                default -> System.out.println("Invalid option. Try again.");
            }

            System.out.println();
        }
    }
}

