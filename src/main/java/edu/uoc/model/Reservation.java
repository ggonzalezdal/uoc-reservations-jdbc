package edu.uoc.model;

import java.time.OffsetDateTime;

public class Reservation {

    private long reservationId;
    private long customerId;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;       // nullable
    private int partySize;
    private String status;
    private String notes;              // nullable
    private OffsetDateTime createdAt;  // set by DB (read-only for inserts)

    public Reservation() {
        // default constructor (needed by some JDBC patterns)
    }

    public Reservation(long customerId,
                       OffsetDateTime startAt,
                       OffsetDateTime endAt,
                       int partySize,
                       String status,
                       String notes) {
        this.customerId = customerId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.partySize = partySize;
        this.status = status;
        this.notes = notes;
    }

    public long getReservationId() { return reservationId; }
    public void setReservationId(long reservationId) { this.reservationId = reservationId; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public OffsetDateTime getStartAt() { return startAt; }
    public void setStartAt(OffsetDateTime startAt) { this.startAt = startAt; }

    public OffsetDateTime getEndAt() { return endAt; }
    public void setEndAt(OffsetDateTime endAt) { this.endAt = endAt; }

    public int getPartySize() { return partySize; }
    public void setPartySize(int partySize) { this.partySize = partySize; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

