package edu.uoc.api;

import edu.uoc.api.dto.ApiDtos.CancelReservationRequest;
import edu.uoc.api.dto.ApiDtos.CreateReservationRequest;
import edu.uoc.api.dto.ApiDtos.CreateReservationResponse;
import edu.uoc.api.dto.ApiDtos.StatusChangeResponse;
import edu.uoc.dao.ReservationDao;
import edu.uoc.service.ReservationService;
import io.javalin.Javalin;

import java.time.OffsetDateTime;

public final class ReservationsRoutes {
    private ReservationsRoutes() {}

    public static void register(Javalin app, ReservationDao reservationDao, ReservationService reservationService) {

        app.get("/reservations", ctx -> ctx.json(reservationDao.findAll()));

        app.post("/reservations", ctx -> {
            CreateReservationRequest body = ApiParsers.readRequiredJsonBody(ctx, CreateReservationRequest.class);

            OffsetDateTime startAt = ApiParsers.parseRequiredOffsetDateTimeField(body.startAt(), "startAt");
            OffsetDateTime endAt = ApiParsers.parseOptionalOffsetDateTimeField(body.endAt(), "endAt");

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

        app.post("/reservations/{id}/confirm", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            boolean changed = reservationService.confirmReservation(id);
            ctx.json(new StatusChangeResponse(id, changed));
        });

        app.post("/reservations/{id}/cancel", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));

            CancelReservationRequest body = null;
            try {
                if (!ctx.body().isBlank()) {
                    body = ApiParsers.readRequiredJsonBody(ctx, CancelReservationRequest.class);
                }
            } catch (Exception ignore) {
                // treat as no reason
            }

            String reason = (body == null) ? null : body.reason();
            boolean changed = reservationService.cancelReservation(id, reason);

            ctx.json(new StatusChangeResponse(id, changed));
        });
    }
}
