package edu.uoc.dto;

import java.time.OffsetDateTime;

public class ReservationListItem {

    private final long reservationId;
    private final long customerId;
    private final String customerName;
    private final OffsetDateTime startAt;
    private final OffsetDateTime endAt;      // nullable
    private final int partySize;
    private final String status;
    private final String notes;             // nullable
    private final OffsetDateTime createdAt;

    public ReservationListItem(long reservationId,
                               long customerId,
                               String customerName,
                               OffsetDateTime startAt,
                               OffsetDateTime endAt,
                               int partySize,
                               String status,
                               String notes,
                               OffsetDateTime createdAt) {
        this.reservationId = reservationId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.startAt = startAt;
        this.endAt = endAt;
        this.partySize = partySize;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public long getReservationId() { return reservationId; }
    public long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public OffsetDateTime getStartAt() { return startAt; }
    public OffsetDateTime getEndAt() { return endAt; }
    public int getPartySize() { return partySize; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "ReservationListItem{" +
                "reservationId=" + reservationId +
                ", customerId=" + customerId +
                ", customerName='" + customerName + '\'' +
                ", startAt=" + startAt +
                ", endAt=" + endAt +
                ", partySize=" + partySize +
                ", status='" + status + '\'' +
                ", notes='" + notes + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}


