package edu.uoc.api;

import com.fasterxml.jackson.databind.SerializationFeature;
import edu.uoc.dao.CustomerDao;
import edu.uoc.dao.ReservationDao;
import edu.uoc.dao.TableDao;
import edu.uoc.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class ApiServer {

    private static final int PORT = 7070;

    public static void main(String[] args) {

        ReservationService reservationService = new ReservationService();
        ReservationDao reservationDao = new ReservationDao();
        CustomerDao customerDao = new CustomerDao();
        TableDao tableDao = new TableDao();

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";

            config.staticFiles.add("/public");

            config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                mapper.findAndRegisterModules();
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }));
        });

        // Routes
        HealthRoutes.register(app);
        CustomersRoutes.register(app, customerDao);
        TablesRoutes.register(app, tableDao, reservationService);
        ReservationsRoutes.register(app, reservationDao, reservationService);

        // Errors (JSON 404 + consistent mapping)
        ApiErrors.register(app);

        app.start(PORT);
        System.out.println("API running on http://localhost:" + PORT);
    }
}
