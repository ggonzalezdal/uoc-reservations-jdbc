package edu.uoc.api;

import io.javalin.Javalin;

/**
 * Minimal HTTP API server for the reservations backend.
 *
 * <p>This is a thin HTTP layer that exposes service/DAO functionality
 * through REST-style endpoints. Business rules remain in the service layer.</p>
 *
 * @since 1.0
 */
public class ApiServer {

    /**
     * Application entry point for running the HTTP server.
     *
     * @param args CLI args (unused)
     */
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        });

        // Health check endpoint for quick diagnostics.
        app.get("/health", ctx -> ctx.json(new HealthResponse("ok")));

        app.start(7070);
        System.out.println("API running on http://localhost:7070");
    }

    /**
     * Simple JSON response for health checks.
     *
     * @param status health status value
     */
    public record HealthResponse(String status) {}
}
