package edu.uoc.dto;

import java.time.OffsetDateTime;

public class ReservationListItem {

    private long reservationId;
    private long customerId;
    private String customerName;
    private OffsetDateTime startAt;
    private int partySize;
    private String status;

    public ReservationListItem(long reservationId,
                               long customerId,
                               String customerName,
                               OffsetDateTime startAt,
                               int partySize,
                               String status) {
        this.reservationId = reservationId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.startAt = startAt;
        this.partySize = partySize;
        this.status = status;
    }

    public long getReservationId() {
        return reservationId;
    }

    public long getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public int getPartySize() {
        return partySize;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return String.format(
                "#%d | %s | %s | party=%d | %s",
                reservationId,
                customerName,
                startAt,
                partySize,
                status
        );
    }
}

