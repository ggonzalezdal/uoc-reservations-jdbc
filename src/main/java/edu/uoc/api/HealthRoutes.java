package edu.uoc.api;

import edu.uoc.api.dto.ApiDtos.HealthResponse;
import io.javalin.Javalin;

public final class HealthRoutes {
    private HealthRoutes() {}

    public static void register(Javalin app) {
        app.get("/health", ctx -> ctx.json(new HealthResponse("ok")));
    }
}
