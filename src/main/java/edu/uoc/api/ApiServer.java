package edu.uoc.api;

import edu.uoc.service.ReservationService;
import io.javalin.Javalin;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

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

        Javalin app = Javalin.create(config -> config.http.defaultContentType = "application/json");

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

        // Basic API error mapping (thin and predictable).
        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("BAD_REQUEST", e.getMessage()));
        });

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
