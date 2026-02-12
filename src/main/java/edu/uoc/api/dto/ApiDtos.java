package edu.uoc.api.dto;

import java.util.List;

public final class ApiDtos {
    private ApiDtos() {}

    public record HealthResponse(String status) {}

    public record CreateReservationRequest(
            long customerId,
            String startAt,
            String endAt,
            int partySize,
            String status,
            String notes,
            List<Long> tableIds
    ) {}

    public record CreateReservationResponse(long reservationId) {}

    public record CancelReservationRequest(String reason) {}

    public record StatusChangeResponse(long reservationId, boolean changed) {}

    public record ErrorResponse(String code, String message, String path) {}
}
