package edu.uoc.model;

import java.time.OffsetDateTime;

public class Reservation {

    private long reservationId;
    private long customerId;
    private OffsetDateTime startAt;
    private int partySize;
    private String status;

    public Reservation() {
        // default constructor (needed by JDBC patterns)
    }

    public Reservation(long customerId,
                       OffsetDateTime startAt,
                       int partySize,
                       String status) {
        this.customerId = customerId;
        this.startAt = startAt;
        this.partySize = partySize;
        this.status = status;
    }

    public long getReservationId() {
        return reservationId;
    }

    public void setReservationId(long reservationId) {
        this.reservationId = reservationId;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(OffsetDateTime startAt) {
        this.startAt = startAt;
    }

    public int getPartySize() {
        return partySize;
    }

    public void setPartySize(int partySize) {
        this.partySize = partySize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
