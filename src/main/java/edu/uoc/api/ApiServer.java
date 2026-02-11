package edu.uoc.api;

import edu.uoc.dao.ReservationDao;
import edu.uoc.dao.CustomerDao;
import edu.uoc.dao.TableDao;

import edu.uoc.service.ReservationService;
import io.javalin.Javalin;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import io.javalin.json.JavalinJackson;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Minimal HTTP API server for the reservations backend.
 *
 * <p>This is a thin HTTP layer that exposes service/DAO functionality
 * through REST-style endpoints. Business rules remain in the service layer.</p>
 *
 * @since 1.0
 */
public class ApiServer {

    private static final int PORT = 7070;

    /**
     * Application entry point for running the HTTP server.
     *
     * @param args CLI args (unused)
     */
    public static void main(String[] args) {
        ReservationService reservationService = new ReservationService();
        ReservationDao reservationDao = new ReservationDao();
        CustomerDao customerDao = new CustomerDao();
        TableDao tableDao = new TableDao();

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";

            config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                mapper.findAndRegisterModules(); // picks up JavaTimeModule from jackson-datatype-jsr310
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO-8601 instead of numbers
            }));
        });

        // Health check endpoint for quick diagnostics.
        app.get("/health", ctx -> ctx.json(new HealthResponse("ok")));

        /**
         * Lists active tables that are available for a time window.
         *
         * Query params:
         * - start (required): ISO-8601 OffsetDateTime (e.g. 2026-02-21T20:30:00+01:00)
         * - end   (optional): ISO-8601 OffsetDateTime
         *
         * Response: JSON array of Table objects.
         */
        app.get("/tables/available", ctx -> {
            OffsetDateTime startAt = readRequiredOffsetDateTimeQuery(ctx.queryParam("start"), "start");
            OffsetDateTime endAt = readOptionalOffsetDateTimeQuery(ctx.queryParam("end"), "end");

            var available = reservationService.listAvailableTables(startAt, endAt);
            ctx.json(available);
        });

        // Lists all tables (active + inactive). Useful for admin/config UI later.
//        app.get("/tables", ctx -> {
//            var tables = reservationService.getAllTables(); // we'll add this method next
//            ctx.json(tables);
//        });

        // Lists reservations joined with customer name (uses ReservationListItem DTO).
        app.get("/reservations", ctx -> ctx.json(reservationDao.findAll()));

        // List customers
        app.get("/customers", ctx -> ctx.json(customerDao.findAll()));

        // Get customer by id
        app.get("/customers/{id}", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));

            var customerOpt = customerDao.findById(id);
            if (customerOpt.isEmpty()) {
                ctx.status(404).json(new ErrorResponse("NOT_FOUND", "Customer not found: " + id));
                return;
            }
            ctx.json(customerOpt.get());
        });

        // Lists all tables (active + inactive). Useful for admin/config UI.
        app.get("/tables", ctx -> ctx.json(tableDao.findAll()));

        /**
         * Creates a reservation and assigns tables in one transaction (transaction-safe).
         *
         * POST /reservations
         * Body: CreateReservationRequest (JSON)
         *
         * Returns:
         * - 201 + { "reservationId": ... } on success
         * - 400 on invalid input
         * - 409 if tables overlap / business conflict
         */
        app.post("/reservations", ctx -> {
            CreateReservationRequest body = ctx.bodyAsClass(CreateReservationRequest.class);

            OffsetDateTime startAt = parseOffsetDateTimeFieldOrThrow(body.startAt(), "startAt");
            OffsetDateTime endAt = parseOptionalOffsetDateTimeField(body.endAt(), "endAt");

            long reservationId = reservationService.createReservationWithTables(
                    body.customerId(),
                    startAt,
                    endAt,
                    body.partySize(),
                    body.status(),
                    body.notes(),
                    body.tableIds()
            );

            ctx.status(201).json(new CreateReservationResponse(reservationId));
        });

/**
 * Confirms a reservation (PENDING -> CONFIRMED).
 *
 * POST /reservations/{id}/confirm
 *
 * Returns:
 * - 200 + { reservationId, changed }
 * - 400 invalid id
 * - 404 reservation not found
 * - 409 invalid lifecycle transition
 */
        app.post("/reservations/{id}/confirm", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));

            boolean changed = reservationService.confirmReservation(id);
            ctx.json(new StatusChangeResponse(id, changed));
        });

        /**
         * Cancels a reservation (-> CANCELLED), preserving history.
         *
         * POST /reservations/{id}/cancel
         * Body (optional): { "reason": "text" }
         *
         * Returns:
         * - 200 + { reservationId, changed }
         * - 400 invalid id
         * - 404 reservation not found
         */
        app.post("/reservations/{id}/cancel", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));

            // Body is optional, so handle empty body safely
            CancelReservationRequest body = null;
            try {
                if (ctx.body() != null && !ctx.body().isBlank()) {
                    body = ctx.bodyAsClass(CancelReservationRequest.class);
                }
            } catch (Exception ignore) {
                // if body is malformed, we'll treat it as no reason
            }

            String reason = (body == null) ? null : body.reason();

            boolean changed = reservationService.cancelReservation(id, reason);
            ctx.json(new StatusChangeResponse(id, changed));
        });

        // Basic API error mapping (thin and predictable).
        app.exception(NumberFormatException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("BAD_REQUEST", "Invalid numeric value"));
        });

        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            String msg = e.getMessage() == null ? "" : e.getMessage();

            if (msg.startsWith("Reservation not found:") || msg.startsWith("Customer not found:")) {
                ctx.status(404).json(new ErrorResponse("NOT_FOUND", msg));
                return;
            }

            ctx.status(400).json(new ErrorResponse("BAD_REQUEST", msg));
        });

//        app.exception(IllegalArgumentException.class, (e, ctx) -> {
//            ctx.status(400).json(new ErrorResponse("BAD_REQUEST", e.getMessage()));
//        });

        app.exception(IllegalStateException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorResponse("CONFLICT", e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace(); // TEMP: shows real error in console while learning

            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Unexpected server error"));
        });




        app.start(PORT);
        System.out.println("API running on http://localhost:" + PORT);
    }

    /**
     * Parses a required ISO-8601 OffsetDateTime query parameter.
     *
     * @param raw raw query param value
     * @param name param name (for error messages)
     * @return parsed OffsetDateTime
     * @throws IllegalArgumentException if missing or invalid
     */
    private static OffsetDateTime readRequiredOffsetDateTimeQuery(String raw, String name) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Missing required query param: " + name);
        }
        return parseOffsetDateTimeOrThrow(raw, name);
    }

    /**
     * Parses an optional ISO-8601 OffsetDateTime query parameter.
     *
     * @param raw raw query param value
     * @param name param name (for error messages)
     * @return parsed OffsetDateTime or null if missing/blank
     * @throws IllegalArgumentException if present but invalid
     */
    private static OffsetDateTime readOptionalOffsetDateTimeQuery(String raw, String name) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseOffsetDateTimeOrThrow(raw, name);
    }

    private static OffsetDateTime parseOffsetDateTimeOrThrow(String raw, String name) {
        try {
            return OffsetDateTime.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid '" + name + "' datetime. Use ISO-8601, e.g. 2026-02-21T20:30:00+01:00"
            );
        }
    }

    private static OffsetDateTime parseOffsetDateTimeFieldOrThrow(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + fieldName);
        }
        try {
            return OffsetDateTime.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid '" + fieldName + "' datetime. Use ISO-8601, e.g. 2026-02-21T20:30:00Z"
            );
        }
    }

    private static OffsetDateTime parseOptionalOffsetDateTimeField(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) return null;
        return parseOffsetDateTimeFieldOrThrow(raw, fieldName);
    }

    /**
     * Request body for creating a reservation and assigning tables in one transaction.
     *
     * <p>Dates must be ISO-8601 OffsetDateTime strings, e.g. "2026-02-21T20:30:00Z"
     * or "2026-02-21T20:30:00+01:00".</p>
     */
    public record CreateReservationRequest(
            long customerId,
            String startAt,
            String endAt,      // optional
            int partySize,
            String status,     // optional
            String notes,      // optional
            java.util.List<Long> tableIds
    ) {}

    /**
     * Response returned after successful creation.
     */
    public record CreateReservationResponse(long reservationId) {}

    /**
     * Optional request body for cancelling a reservation.
     */
    public record CancelReservationRequest(String reason) {}

    /**
     * Standard response for status-changing operations.
     *
     * @param reservationId affected reservation id
     * @param changed true if the status changed now, false if it was already in that status (idempotent)
     */
    public record StatusChangeResponse(long reservationId, boolean changed) {}

    /**
     * Simple JSON response for health checks.
     *
     * @param status health status value
     */
    public record HealthResponse(String status) {}

    /**
     * Standard JSON error response.
     *
     * @param code machine-readable error code
     * @param message human-readable message
     */
    public record ErrorResponse(String code, String message) {}
}
