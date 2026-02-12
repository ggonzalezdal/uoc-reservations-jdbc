package edu.uoc.api;

import edu.uoc.api.dto.ApiDtos.ErrorResponse;
import io.javalin.Javalin;

public final class ApiErrors {
    private ApiErrors() {}

    public static void register(Javalin app) {

        app.exception(NumberFormatException.class, (e, ctx) ->
                ctx.status(400).json(new ErrorResponse("BAD_REQUEST", "Invalid numeric value", ctx.path()))
        );

        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            String msg = (e.getMessage() == null) ? "" : e.getMessage();

            if (msg.startsWith("Reservation not found:") || msg.startsWith("Customer not found")) {
                ctx.status(404).json(new ErrorResponse("NOT_FOUND", msg, ctx.path()));
                return;
            }

            ctx.status(400).json(new ErrorResponse("BAD_REQUEST", msg, ctx.path()));
        });

        app.exception(IllegalStateException.class, (e, ctx) ->
                ctx.status(409).json(new ErrorResponse("CONFLICT", e.getMessage(), ctx.path()))
        );

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Unexpected server error", ctx.path()));
        });

        app.error(404, ctx -> ctx.json(
                new ErrorResponse("NOT_FOUND", "Route not found: " + ctx.path(), ctx.path())
        ));
    }
}

