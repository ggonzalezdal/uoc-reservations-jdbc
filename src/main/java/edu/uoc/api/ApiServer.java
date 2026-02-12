package edu.uoc.api;

import edu.uoc.dao.CustomerDao;
import edu.uoc.dao.ReservationDao;
import edu.uoc.dao.TableDao;
import edu.uoc.service.ReservationService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import com.fasterxml.jackson.databind.SerializationFeature;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import io.javalin.http.Context;

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

        // Services/DAOs (thin API layer; business logic stays in service).
        ReservationService reservationService = new ReservationService();
        ReservationDao reservationDao = new ReservationDao();
        CustomerDao customerDao = new CustomerDao();
        TableDao tableDao = new TableDao();

        // Javalin setup
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";

            // Configure Jackson to support OffsetDateTime (ISO-8601) properly.
            config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                mapper.findAndRegisterModules(); // picks up JavaTimeModule (jsr310)
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO-8601 strings
            }));
        });

        // ---------- Routes ----------

        // Health check endpoint for quick diagnostics.
        app.get("/health", ctx -> ctx.json(new HealthResponse("ok")));

        // Lists active tables that are available for a time window.
        // Query params:
        // - start (required): ISO-8601 OffsetDateTime (e.g. 2026-02-21T20:30:00+01:00)
        // - end   (optional): ISO-8601 OffsetDateTime
        app.get("/tables/available", ctx -> {
            OffsetDateTime startAt = readRequiredOffsetDateTimeQuery(ctx.queryParam("start"), "start");
            OffsetDateTime endAt = readOptionalOffsetDateTimeQuery(ctx.queryParam("end"), "end");

            var available = reservationService.listAvailableTables(startAt, endAt);
            ctx.json(available);
        });

        // Lists all tables (active + inactive). Useful for admin/config UI.
        app.get("/tables", ctx -> ctx.json(tableDao.findAll()));

        // Lists reservations joined with customer name (ReservationListItem DTO).
        app.get("/reservations", ctx -> ctx.json(reservationDao.findAll()));

        // List customers
        app.get("/customers", ctx -> ctx.json(customerDao.findAll()));

        // Get customer by id
        app.get("/customers/{id}", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));

            var customerOpt = customerDao.findById(id);
            if (customerOpt.isEmpty()) {
                ctx.status(404).json(new ErrorResponse("NOT_FOUND", "Customer not found: " + id, ctx.path()));
                return;
            }

            ctx.json(customerOpt.get());
        });

        // Creates a reservation and assigns tables in one transaction (transaction-safe).
        // POST /reservations
        // Body: CreateReservationRequest JSON
        app.post("/reservations", ctx -> {
            CreateReservationRequest body = readRequiredJsonBody(ctx, CreateReservationRequest.class);

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

        // Confirms a reservation (PENDING -> CONFIRMED).
        // POST /reservations/{id}/confirm
        app.post("/reservations/{id}/confirm", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));

            boolean changed = reservationService.confirmReservation(id);
            ctx.json(new StatusChangeResponse(id, changed));
        });

        // Cancels a reservation (-> CANCELLED), preserving history.
        // POST /reservations/{id}/cancel
        // Optional body: { "reason": "text" }
        app.post("/reservations/{id}/cancel", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));

            CancelReservationRequest body = null;
            try {
                if (!ctx.body().isBlank()) {
                    body = readRequiredJsonBody(ctx, CancelReservationRequest.class);
                }
            } catch (Exception ignore) {
                // If body is malformed, treat it as no reason.
            }

            String reason = (body == null) ? null : body.reason();

            boolean changed = reservationService.cancelReservation(id, reason);
            ctx.json(new StatusChangeResponse(id, changed));
        });


        // ---------- Error mapping (thin + predictable) ----------

        // Path params like /customers/{id} can throw this.
        app.exception(NumberFormatException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("BAD_REQUEST", "Invalid numeric value", ctx.path()));
        });

        // Validation / business input errors
        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            String msg = (e.getMessage() == null) ? "" : e.getMessage();

            // Map "not found" style business errors to 404
            if (msg.startsWith("Reservation not found:") || msg.startsWith("Customer not found")) {
                ctx.status(404).json(new ErrorResponse("NOT_FOUND", msg, ctx.path()));
                return;
            }

            ctx.status(400).json(new ErrorResponse("BAD_REQUEST", msg, ctx.path()));
        });

        // Business conflicts (overlap, invalid lifecycle transitions, etc.)
        app.exception(IllegalStateException.class, (e, ctx) -> {
            ctx.status(409).json(new ErrorResponse("CONFLICT", e.getMessage(), ctx.path()));
        });

        // Fallback
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace(); // TEMP: show real error in console while learning
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Unexpected server error", ctx.path()));
        });

        // Unknown routes should return JSON (not Javalin default HTML)
        app.error(404, ctx -> ctx.json(
                new ErrorResponse("NOT_FOUND", "Route not found: " + ctx.path(), ctx.path())
        ));


        // Start server
        app.start(PORT);
        System.out.println("API running on http://localhost:" + PORT);
    }

    // ---------- Query param parsing helpers ----------

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

    // ---------- JSON body field parsing helpers ----------

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

    private static <T> T readRequiredJsonBody(Context ctx, Class<T> clazz) {
        String raw = ctx.body();
        if (raw.isBlank()) {
            throw new IllegalArgumentException("Request body is required");
        }
        try {
            return ctx.bodyAsClass(clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed JSON body");
        }
    }


    // ---------- DTOs for API ----------

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
            List<Long> tableIds
    ) {}

    /**
     * Response returned after successful creation.
     *
     * @param reservationId generated reservation id
     */
    public record CreateReservationResponse(long reservationId) {}

    /**
     * Optional request body for cancelling a reservation.
     *
     * @param reason cancellation reason (optional)
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
    public record ErrorResponse(String code, String message, String path) {}
}
