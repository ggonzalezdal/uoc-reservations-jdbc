package edu.uoc.api;

import edu.uoc.dao.TableDao;
import edu.uoc.service.ReservationService;
import io.javalin.Javalin;

import java.time.OffsetDateTime;

public final class TablesRoutes {
    private TablesRoutes() {}

    public static void register(Javalin app, TableDao tableDao, ReservationService reservationService) {

        app.get("/tables", ctx -> ctx.json(tableDao.findAll()));

        app.get("/tables/available", ctx -> {
            OffsetDateTime startAt = ApiParsers.readRequiredOffsetDateTimeQuery(ctx.queryParam("start"), "start");
            OffsetDateTime endAt = ApiParsers.readOptionalOffsetDateTimeQuery(ctx.queryParam("end"), "end");

            ctx.json(reservationService.listAvailableTables(startAt, endAt));
        });
    }
}
